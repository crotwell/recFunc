package edu.sc.seis.receiverFunction.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public class HKStackIterator implements Iterator {

    public HKStackIterator(ResultSet rs, JDBCHKStack jdbcHKStack) {
        this.rs = rs;
        this.jdbcHKStack = jdbcHKStack;
    }

    public boolean hasNext() {
        checkNext();
        return next != null;
    }

    public Object next() {
        System.out.println("extract "+num++);
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

    int num = 1;
    
    Object next = null;

    ResultSet rs;

    JDBCHKStack jdbcHKStack;
}
