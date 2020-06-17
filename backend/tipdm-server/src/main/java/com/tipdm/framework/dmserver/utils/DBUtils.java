package com.tipdm.framework.dmserver.utils;

import com.alibaba.datax.core.Engine;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.drinkjava2.jdialects.springsrc.utils.Assert;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.tipdm.framework.common.utils.FileKit;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.dmserver.exception.ConnectionException;
import com.tipdm.framework.dmserver.websocket.SocketServer;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.model.dmserver.DataType;
import com.tipdm.framework.persist.jdbc.DynamicLoadDriver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;


/**
 * Created by TipDM on 2017/1/20.
 * E-mail:devp@tipdm.com
 */
public final class DBUtils {

    private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void dataSync(JSONObject job) throws Throwable {
        File tmpfile = new File(StringKit.uuid());
        try {

            tmpfile.createNewFile();
            FileKit.writeStringToFile(tmpfile, job.toJSONString(), com.tipdm.framework.common.Constants.CHARACTER);
            Process pr = Runtime.getRuntime().exec(new String[]{"python", System.getProperty("datax.home") + "/bin/datax.py",  tmpfile.getAbsolutePath()});
            ProcessCallable errorCallable = new ProcessCallable(pr.getErrorStream(), "Error");
            ProcessCallable outputCallable = new ProcessCallable(pr.getInputStream(), "Output");
            executorService.submit(errorCallable);
            executorService.submit(outputCallable);

            pr.waitFor();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            FileKit.forceDelete(tmpfile);
        }

    }

    public static boolean dataSync(String accessToken, DataTable dataTable, JSONObject job) {

        boolean res = false;
        File tmpfile = new File(StringKit.uuid());
        String msg = "";
        Map<String, String> result = new HashMap<>();
        result.put("data", dataTable.getShowName());
        try {
            tmpfile.createNewFile();
            FileKit.writeStringToFile(tmpfile, job.toJSONString(), com.tipdm.framework.common.Constants.CHARACTER);
            Process pr = Runtime.getRuntime().exec(new String[]{"python", System.getProperty("datax.home") + "/bin/datax.py",  tmpfile.getAbsolutePath()});
            ProcessCallable errorCallable = new ProcessCallable(pr.getErrorStream(), "Error");
            ProcessCallable outputCallable = new ProcessCallable(pr.getInputStream(), "Output");
            Future<String> error = executorService.submit(errorCallable);
            Future<String> output = executorService.submit(outputCallable);
            pr.waitFor();

            msg = "数据表" + dataTable.getTableName() + "同步完成，" + output.get(1, TimeUnit.DAYS) + "\n" + error.get(1, TimeUnit.DAYS);
            res = true;
        } catch (Throwable throwable) {
            msg = "数据表" + dataTable.getTableName() + "同步失败，错误信息：" + ExceptionUtils.getRootCauseMessage(throwable);
        } finally {
            try {
                FileKit.forceDelete(tmpfile);
            } catch (IOException e) {

            }

            if (res) {
                result.put("status", "SUCCESS");
                result.put("message", msg);
            } else {
                result.put("status", "FAIL");
                result.put("message", msg);
            }
            SocketServer.sendDataSyncResult(accessToken, JSON.toJSONString(result));
        }
        return res;
    }

    public static class ProcessCallable implements Callable<String> {

        InputStream is;
        String type;

        public ProcessCallable(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public String call() throws Exception {
            try {
                StringBuffer stringBuffer = new StringBuffer();
                InputStreamReader isr = new InputStreamReader(is,"UTF8");
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (type.equals("Error")) {
                        logger.error(line);
                    } else {
                        logger.info(line);
                    }
                    if(StringKit.containsAny(line, "任务总计耗时", "读出记录总数", "读写失败总数")) {
                        stringBuffer.append(line).append(";");
                    }
                }

                return stringBuffer.toString();
            } catch (IOException ioe) {
                logger.error(ExceptionUtils.getStackTrace(ioe));
                return null;
            }
        }
    }


    static
    public JSONObject getReaderTemplate(String reader) throws IOException {

        String dataXHome = PropertiesUtil.getValue("/sysconfig/system.properties", "dataX.home");
        System.setProperty("datax.home", dataXHome);
        String template = FileKit.readFileToString(new File(dataXHome, "/plugin/reader/" + reader + "/plugin_job_template.json"));
        return JSONObject.parseObject(template);

    }

    static
    public JSONObject initJob(String url, String user, String password, String table, boolean truncate) {
        JSONObject root = JSONObject.parseObject(JOB_TEMPLATE);
        JSONArray content = root.getJSONObject("job").getJSONArray("content");
        JSONObject writer = content.getJSONObject(0).getJSONObject("writer");
        JSONObject parameter = writer.getJSONObject("parameter");
        parameter.put("password", password);
        parameter.put("username", user);
        Map<String, Object> connection = new HashMap<>();
        connection.put("jdbcUrl", url);
        connection.put("table", new String[]{table});
        parameter.getJSONArray("connection").set(0, connection);
        parameter.put("column", new String[]{"*"});
        if (truncate) {
            parameter.put("preSql", new String[] {"delete from " + table});
        }
        writer.put("parameter", parameter);

        return root;
    }

    static
    public Map<String, Object> testConnection(com.tipdm.framework.controller.dmserver.dto.datasource.Connection conn) throws ClassNotFoundException, SQLException {
        String url = conn.getUrl();
        DataBase dataBase = validURL(url);
        Map<String, Object> result = new HashMap<>();
        Class.forName(dataBase.getDriverClass());
        String sql = dataBase.limit(conn.getSql());
        logger.info("execute test SQL: {}", sql);
        try(java.sql.Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUserName(), conn.getPassword())) {
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            List<DataColumn> columns = new ArrayList<>();
            try(Statement statement = connection.createStatement()){
                try(ResultSet resultSet = statement.executeQuery(sql)) {
                    if (resultSet != null) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        boolean first = true;
                        while (resultSet.next()) {
                            Map<String, Object> tmpData = new HashMap<>();
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                String columnName = metaData.getColumnName(i);
                                Object value = resultSet.getObject(i);
                                tmpData.put(columnName, value);
                                if(first){
                                    DataColumn column = new DataColumn();
                                    column.setName(columnName);
                                    column.setDataType(DataType.getDataType(String.valueOf(value)));
                                    columns.add(column);
                                }
                            }
                            data.add(tmpData);
                            first = false;
                        }
                    }
                }
            }
            //写入缓存，5分钟有效
            com.tipdm.framework.common.utils.RedisUtils.set(conn.toString(), "", 60 * 5L);
            result.put("data", data);
            result.put("columns", columns);
        }
        return result;
    }
    private static List<DataBase> dataBaseList;

    static {
        InputStream in = PagerKit.class.getResourceAsStream("/sysconfig/dbSupport.config");
        try {
            String text = IOUtils.toString(in, com.tipdm.framework.common.Constants.CHARACTER);
            dataBaseList = JSON.parseObject(text, new TypeReference<List<DataBase>>() {
            });
        } catch (IOException e) {
            logger.error("配置文件[dbSupport.config]解析错误，{}", e.getMessage());
        }
    }

    public static class DataBase {

        private String connectionUrlPrefix;

        private String jarFileName;

        private String driverClass;

        private String pageSQL;

        private String reader;

        public String getConnectionUrlPrefix() {
            return connectionUrlPrefix;
        }

        public void setConnectionUrlPrefix(String connectionUrlPrefix) {
            this.connectionUrlPrefix = connectionUrlPrefix;
        }

        public String getJarFileName() {
            return jarFileName;
        }

        public void setJarFileName(String jarFileName) {
            this.jarFileName = jarFileName;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getPageSQL() {
            return pageSQL;
        }

        public void setPageSQL(String pageSQL) {
            this.pageSQL = pageSQL;
        }

        public String getReader() {
            return reader;
        }

        public void setReader(String reader) {
            this.reader = reader;
        }

        public String limit(String sql) {
            Assert.notNull(sql, "SQL can not be null");
            try {
                String dbType = JdbcUtils.getDbType(connectionUrlPrefix, driverClass);
                return PagerKit.limit(sql, dbType, 0, 100);
            } catch (Exception ex) {
                Template template = Mustache.compiler().escapeHTML(false).compile(this.pageSQL);
                sql = MessageFormatter.format(" ({}) as tmp ", sql).getMessage();
                Map<String, Object> params = new HashMap<>();
                params.put("table", sql);
                return template.execute(params);
            }
        }
    }

    static
    public DataBase validURL(String url) throws SQLException {
        if (StringKit.isBlank(url)) {
            throw new IllegalArgumentException("数据源连接字符串不能为空！");
        }

        Optional<DataBase> optional = dataBaseList.stream().filter(x -> url.startsWith(x.getConnectionUrlPrefix())).findFirst();
        if (optional.isPresent()) {
            DataBase dataBase = optional.get();
            try {
                Class.forName(dataBase.getDriverClass());
            } catch (ClassNotFoundException ex) {
                File classes = new File(DBUtils.class.getResource("/").getFile());
                File jarFile = new File(classes.getParentFile(), "lib/" + dataBase.getJarFileName());
                URL resource = null;
                try {
                    resource = jarFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    logger.error(e.getMessage());
                }
                DynamicLoadDriver.loadDriver(resource, dataBase.getDriverClass());
            }
            return dataBase;
        } else {
            throw new SQLException("不支持的数据库类别，请前往/sysconfig/dbSupport.config添加配置");
        }
    }

    static
    public void testConnection(String url, String username, String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception ex) {
            throw new ConnectionException("无法连接到目标数据库！");
        } finally {
            if (null != conn) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private final static String JOB_TEMPLATE = "{\n" +
            "      \"job\": {\n" +
            "        \"setting\": {\n" +
            "          \"speed\": {\n" +
            "            \"channel\": \"5\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"content\": [\n" +
            "          {\n" +
            "            \"reader\": {},\n" +
            "            \"writer\": {\"name\": \"postgresqlwriter\",\n" +
            "                    \"parameter\": {\n" +
            "                        \"column\": [],\n" +
            "                        \"connection\": [\n" +
            "                            {\n" +
            "                                \"jdbcUrl\": \"\",\n" +
            "                                \"table\": []\n" +
            "                            }\n" +
            "                        ],\n" +
            "                        \"password\": \"\",\n" +
            "                        \"username\": \"\"\n" +
            "                    }}\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }";
}