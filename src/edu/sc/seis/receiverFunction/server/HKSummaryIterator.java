package edu.sc.seis.receiverFunction.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import edu.sc.seis.receiverFunction.SumHKStack;


public class HKSummaryIterator implements Iterator {

    public HKSummaryIterator(ResultSet rs, JDBCSummaryHKStack jdbcSumHKStack, boolean autoCommit) {
        this.rs = rs;
        this.jdbcSumHKStack = jdbcSumHKStack;
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
        return nextSummary();
    }
    
    public SumHKStack nextSummary() {
        num++;
        checkNext();
        SumHKStack tmp = next;
        next = null;
        return tmp;
    }

    public void remove() {
        throw new RuntimeException("Not implemented");
    }

    public ResultSet getResultSet() {
        return rs;
    }
    
    public void close() throws SQLException {
        rs.close();
        jdbcSumHKStack.getConnection().setAutoCommit(autoCommit);
    }
    
    private void checkNext() {
        try {
            if(next == null && rs.next()) {
                next = jdbcSumHKStack.extract(rs, true);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    boolean autoCommit;

    int num = 1;
    
    SumHKStack next = null;

    ResultSet rs;

    JDBCSummaryHKStack jdbcSumHKStack;
}
