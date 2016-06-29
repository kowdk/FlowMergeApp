package com.xutao.mergeApp.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.po.Record;

public class OscarDao {
	
	// 数据库连接基本参数
	private static final String userName = "TEST_USR";
	private static final String password = "test_usr";
	private static final String dbName = "LOGDB";
	private static final String host = "10.157.192.29";
	private static final String port = "2010";
    
	private static Logger logger = Logger.getLogger(OscarDao.class);
	
    /**
     * 创建数据库连接
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private static Connection getConnection() throws ClassNotFoundException, SQLException {
        Connection conn = null;
        Properties p = new Properties();

        Class.forName("com.oscar.cluster.BulkDriver");
        String url = "jdbc:oscarclusterbulk://" + host + ":" + port + "/" + dbName;
        p.setProperty("user", userName);
        p.setProperty("password", password);
        p.setProperty("BulkBufferSize", "1000");
        p.setProperty("NodeLoginTimeout", "60");
        conn = DriverManager.getConnection(url, p);

        return conn;
    }

    /**
     * 使用JDBC大容量导入接口加载数据
     * 
     * @throws SQLException
     */
    public static void batchInsert(List<Record> list, String insertSql) {
    	if(list.size() == 0)
    		return;
    	
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            //建立数据库连接，建议使用连接池实现
            conn = getConnection();
            pStmt = conn.prepareStatement(insertSql);

            //手动提交事务
            conn.setAutoCommit(false);

            //绑定数据                
            //绑定一批数据， 每次绑定的数据不要太小，以保证数据的压缩率，一般不少于表的PACKET大小乘以分区个数
            for (Record r : list) {
            	int index = 1;
            	pStmt.setLong(index++, r.getTimestamp());
            	List<Long> tmp = r.getKeyFields();
				for (int i = 0; i < tmp.size(); i++) {
					pStmt.setLong(index++, tmp.get(i));
				}
				tmp = r.getStatFields();
				for (int i = 0; i < tmp.size(); i++) {
					pStmt.setLong(index++, tmp.get(i));
				}
				pStmt.addBatch();
			}

            //提交数据
            pStmt.execute();
            //提交事务
            conn.commit();

            //清理导入对象
            pStmt.clearBatch();
            pStmt.close();
            pStmt = null;
        } catch (ClassNotFoundException exp) {
            logger.error(exp);
        } catch (SQLException exp) {
            //出错回滚事务，建议把这批出错数据重新提交几次，如果最终还是失败，将这批出错数据写到磁盘中保存。
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException exp1) {
                logger.error(exp1);
            }
            logger.error(exp);
        } finally {
            //关闭连接
            try {
                if (pStmt != null) {
                    pStmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException exp) {
               logger.error(exp);
            }
        }
    }
    
}
