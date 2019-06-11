/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ee.ut.merzin.kammkala2.helpers;

import ee.ut.merzin.kammkala2.controller.DataBaseConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author markko
 */
public class KammkalaLogger {

    public static final int DEBUG = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;
    public static final int FATAL = 3;
    public static int loglevel = 1;

    public static void log(int level, String lclass, Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();
        sw.flush();
        log(level, lclass, ex.getMessage(), sw.toString());
        pw.close();
        try {
            sw.close();
        } catch (IOException ex1) {            
        }
    }

    public static void log(int level, String lclass, String message, String stacktrace) {
        try {
            Connection conn = DataBaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            PreparedStatement pstmt = conn.prepareStatement("insert into log (time, level, class, message, stacktrace) values (?, ?, ?, ?, ?)");
            
            java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
            pstmt.setTimestamp(1, ts);
            pstmt.setInt(2, level);
            pstmt.setString(3, lclass);
            pstmt.setString(4, message);
            pstmt.setString(5, stacktrace);
            
            pstmt.executeUpdate();
            conn.commit();
            pstmt.close();
        } catch (Exception ex) {
            log2StdErr(level, lclass, message, stacktrace);
        }

    }

    public static void log2StdErr(int level, String lclass, String message, String stacktrace) {
        if (level >= KammkalaLogger.loglevel) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.err.println(dateFormat.format(date) + ": " + lclass);
            System.err.println(message);
            System.err.println();
            if (stacktrace != null) {
                System.err.println(stacktrace);
                System.err.println();
            }
        }
    }
}
