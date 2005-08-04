package edu.sc.seis.receiverFunction.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;


/**
 * @author crotwell
 * Created on Jun 22, 2005
 */
public class JDBCHKRealImag extends JDBCTable {

    /**
     * @throws SQLException
     *
     */
    public JDBCHKRealImag(Connection conn) throws SQLException {
        super("hkrealimag", conn);
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
        hkrealimag_seq = new JDBCSequence(conn, getTableName()+"Seq");
    }
    
    public int put(CmplxArray2D analyticPs, CmplxArray2D analyticPpPs, CmplxArray2D analyticPsPs ) throws IOException, SQLException {
        int index = 1;
        int seq = hkrealimag_seq.next();
        put.setInt(index++, seq);
        index = insert(analyticPs, put, index);
        index = insert(analyticPpPs, put, index);
        index = insert(analyticPsPs, put, index);
        put.execute();
        return seq;
    }
    
    public CmplxArray2D[] get(int hkstack_id, int numH, int numK) throws SQLException, IOException {
        get.setInt(1, hkstack_id);
        ResultSet rs = get.executeQuery();
        return extractData(rs, numH, numK);
    }
    
    protected int insert(CmplxArray2D data, PreparedStatement stmt, int index)
            throws IOException, SQLException {
        ByteArrayOutputStream real = new ByteArrayOutputStream();
        DataOutputStream realdos = new DataOutputStream(real);
        ByteArrayOutputStream imag = new ByteArrayOutputStream();
        DataOutputStream imagdos = new DataOutputStream(imag);
        for(int i = 0; i < data.getXLength(); i++) {
            for(int j = 0; j < data.getYLength(); j++) {
                realdos.writeDouble(data.getReal(i, j));
                imagdos.writeDouble(data.getImag(i, j));
            }
        }
        byte[] valBytes = real.toByteArray();
        stmt.setBytes(index++, valBytes);
        valBytes = imag.toByteArray();
        stmt.setBytes(index++, valBytes);
        return index;
    }
    
    public CmplxArray2D[] extractData(ResultSet rs, int numH, int numK) throws SQLException, IOException {
        CmplxArray2D[] data = new CmplxArray2D[3];
        for(int i = 0; i < data.length; i++) {
            data[i] = new CmplxArray2D(numH, numK);
        }
        // Ps
        byte[] dataBytes = rs.getBytes("psreal");
        DataInputStream realdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        dataBytes = rs.getBytes("psimag");
        DataInputStream imagdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        for(int i = 0; i < data[0].getXLength(); i++) {
            for(int j = 0; j < data[0].getYLength(); j++) {
                data[0].setReal(i, j, (float)realdis.readDouble());
                data[0].setImag(i, j, (float)imagdis.readDouble());
            }
        }
        realdis.close();
        imagdis.close();
        // PpPs
        dataBytes = rs.getBytes("pppsreal");
        realdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        dataBytes = rs.getBytes("pppsImag");
        imagdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        for(int i = 0; i < data[1].getXLength(); i++) {
            for(int j = 0; j < data[1].getYLength(); j++) {
                data[1].set(i, j, new Cmplx(realdis.readDouble(), imagdis.readDouble()));
            }
        }
        realdis.close();
        imagdis.close();
        // Ps
        dataBytes = rs.getBytes("pspsreal");
        realdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        dataBytes = rs.getBytes("pspsimag");
        imagdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        for(int i = 0; i < data[2].getXLength(); i++) {
            for(int j = 0; j < data[2].getYLength(); j++) {
                data[2].set(i, j, new Cmplx(realdis.readDouble(), imagdis.readDouble()));
            }
        }
        realdis.close();
        imagdis.close();
        return data;
    }
    
    PreparedStatement put, get;
    
    JDBCSequence hkrealimag_seq;
}
