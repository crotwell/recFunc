package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public class HKStackIterator implements Iterator {

    public HKStackIterator(ResultSet rs, JDBCHKStack jdbcHKStack, boolean autoCommit) {
        this.rs = rs;
        this.jdbcHKStack = jdbcHKStack;
        this.autoCommit = autoCommit;
    }

    public boolean hasNext() {
        checkNext();
        if (next == null) {
            System.out.println("extracted "+num);
        }
        return next != null;
    }

    public Object next() {
        num++;
        checkNext();
        Object tmp = next;
        next = null;
        return tmp;
    }

    public void remove() {
    // no impl
    }

    public ResultSet getResultSet() {
        return rs;
    }
    
    public void close() throws SQLException {
        rs.close();
        jdbcHKStack.getConnection().setAutoCommit(autoCommit);
    }
    
    private void checkNext() {
        try {
            if(next == null && rs.next()) {
                next = jdbcHKStack.extract(rs);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    boolean autoCommit;

    int num = 1;
    
    Object next = null;

    ResultSet rs;

    JDBCHKStack jdbcHKStack;

}
