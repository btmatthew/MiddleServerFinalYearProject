

package middleserver;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Matthew Bulat
 */
public class AdminCheck {
    SystemConfiguration data = new SystemConfiguration();
    String check = data.getAdminDatabaseCredentials("dataWithPassword");
    String[] splitter=check.split(",");
    
    final private String host = "jdbc:derby://"+splitter[1];
    final private String username =splitter[3];
    final private String password =splitter[4];
    final private Logging log = new Logging();
    final private PasswordHash hash = new PasswordHash();
    
    protected String adminLogin(String[] data){
        String result=null;
        String admintoken;
        String[] credentials = data;
        TokenGenAdmin token = new TokenGenAdmin();
        log.writeLog("Admin access requested by "+credentials[1]);
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            log.writeLog("Database query requested " + SQL);
            while(rs.next()){
                String id = rs.getString("ADMINID");
                 if(id.equals(credentials[1])){
                 String inDBpass = rs.getString("PASSWORD");
                 boolean checkPassword=false;
                        try{
                            checkPassword = hash.validatePassword(credentials[2],inDBpass);
                            }catch(NoSuchAlgorithmException | InvalidKeySpecException ex ){
                                log.writeLog("Password validation "+ ex);
                            }
                        if(checkPassword){                                
                                admintoken=token.randomString();
                                rs.updateString("TOKEN", admintoken);
                                rs.updateRow();
                                result="pass"+","+admintoken;
                                log.writeLog("Admin access successful for"+" "+credentials[1]+"Fresh new token generated");
                                rs.afterLast();
                        }else{
                            result="fail";
                            log.writeLog("Admin access failed for"+" "+credentials[1]);
                            rs.afterLast();
                        }
                     
                    }else{
                     result="fail";
                     log.writeLog("Admin access failed for"+" "+credentials[1]);
                     rs.afterLast();
                 }
                 
            }
            stmt.close();
        }catch (SQLException e){
            log.writeLog("SQL Exception adminLogin "+e);
        }
        return result;
    }
    protected String confirmAdmin(String[] data){
        String confirm = null;
        String[] credentials = data;
         try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            log.writeLog("Database query requested"+" "+SQL);
            while(rs.next()){
                String id = rs.getString("ADMINID");
                 if(id.equals(credentials[0])){
                    String inDBtoken = rs.getString("TOKEN");
                    if(inDBtoken.equals(credentials[1])){
                        confirm="proceed"; 
                        rs.afterLast();
                        log.writeLog("Admin "+credentials[0]+" token confirmed");
                    }else{
                        confirm="AuthFailed";
                        rs.afterLast();
                        log.writeLog("Admin "+credentials[0]+" provided incorrect token");
                    }
                 }else if(!rs.next()){
                     confirm="AuthFailed";
                 }
            }
            stmt.close();
            }catch (SQLException e){
             log.writeLog("SQL Exception confirmAdmin "+e);
             
        }
        return confirm;
    } 
    //This method allows admin to update his password.
    protected String passwordUpdate(String[] data){
        String confirm=null;
        String[] _data=data;
        int _dataLength=_data.length;
        
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            log.writeLog("Admin password updated");
            while(rs.next()){
                String id = rs.getString("ADMINID");
                if(_data[_dataLength-2].equals(id)){
                    String hashed=null;
                        try{
                            hashed =hash.createHash(_data[1]);
                            System.out.println(hashed);
                        }catch(NoSuchAlgorithmException |InvalidKeySpecException ex ){
                            log.writeLog("Excemption when updating admin password by hash class"+ ex);
                        }
                        rs.updateString("PASSWORD", hashed);
                        rs.updateRow();
                        rs.afterLast();
                        confirm="AdminPasswordUpdated";
                }
            }
            stmt.close();
            if(confirm==null){
                confirm="AdminPasswordUpdatedFail";
            }
        }catch (SQLException e){
             log.writeLog("SQL Exception passwordUpdate "+e);
        }
        return confirm;
    }
    protected String newAdmin(String[] data){
        String userName=null;
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            rs.moveToInsertRow();
            rs.updateString("ADMINID",data[1]);
            String hashed=null;
            try{
                hashed=hash.createHash(data[2]);
                }catch(NoSuchAlgorithmException |InvalidKeySpecException ex ){
                    log.writeLog("newAdmin hash Exception "+ ex);
                }
            rs.updateString("PASSWORD", hashed);
            rs.updateString("TOKEN", "no_token");
            rs.insertRow();
            log.writeLog("New admin added");
            stmt.close();
            userName="UserNameAdded";
        }catch (SQLException e){
             log.writeLog("SQL Exception newAdmin "+e);
        }
        return userName;
    }
}
