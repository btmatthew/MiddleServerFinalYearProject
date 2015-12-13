package middleserver;

//Imports:
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
public class LoginCheck {
    //Purpose of this method is to confirm users details.
    private Logging log = new Logging();
    private java.util.Date date= new java.util.Date();
    SystemConfiguration data = new SystemConfiguration();
    String check = data.getUserDatabaseCredentials("dataWithPassword");
    String[] splitter=check.split(",");
    private final String host = "jdbc:derby://"+splitter[1];
    private final String username = splitter[3];
    private final String password = splitter[4];
    private PasswordHash hash = new PasswordHash();
    
    protected String[] login(String[] data){
        String[] reply=new String[3];
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            while(rs.next()){
                String id = rs.getString("USERID");
                if(id.equals(data[0])){
                    Boolean blocked = rs.getBoolean("BLOCKED"); 
                    int noOfTries = rs.getInt("TRIES");
                    if(blocked){
                        log.writeLog("Account"+" "+data[0]+" "+"is locked but tried to access the system");
                        reply[0]="#*blocked*#";
                        rs.afterLast();
                    }else if(noOfTries==9){
                        log.writeLog("Accout"+" "+data[0]+" "+"has been marked as locked, after trying to login 10 times");
                        rs.updateBoolean("BLOCKED", true);
                        rs.updateRow();
                        rs.afterLast();
                        reply[0]="#*acc_blocked*#";
                    }else{
                        boolean checkPassword=false;
                        try{
                            String pass = rs.getString("PASSW0RD");
                            checkPassword = hash.validatePassword(data[1],pass);
                            }catch(NoSuchAlgorithmException | InvalidKeySpecException ex ){
                                log.writeLog("User login password validation error "+ ex);
                            }
                        if(checkPassword){
                            log.writeLog("User"+" "+data[0]+" "+"logged in successfully ");
                            rs.updateInt("TRIES", 0);
                            rs.updateRow();
                            String token = rs.getString("TOKEN");
                            
                            if(token.equals("no_token")){
                                log.writeLog("User"+" "+data[0]+" "+"doesn't have token, generating new token");
                                TokenGenUser tokengen = new TokenGenUser();
                                String newToken = tokengen.randomString();
                                rs.updateString("TOKEN", newToken);
                                rs.updateRow();
                                reply[0]="#*new_token*#";
                                reply[1]=newToken;
                                rs.afterLast();
                            }else{
                                reply[0]="#*ok*#";
                                reply[1]=token;
                               rs.afterLast();
                            }
                        }else{
                            log.writeLog("User"+" "+data[0]+" "+"sent wrong password, value of number of tries was increased to"+" "+noOfTries+1);
                            noOfTries=noOfTries+1;
                            rs.updateInt("TRIES", noOfTries);
                            rs.updateRow();
                            rs.afterLast();
                            reply[2]= Integer.toString(noOfTries);
                            reply[0]="#*try_again*#";
                        }
                    }
                }else if(rs.isLast()){
                    log.writeLog("User"+" "+data[0]+" "+"does not exists");
                    reply[0]="#*try_again_user*#";
                }
            }
            stmt.close();
        }catch (SQLException e) {
            log.writeLog("Login SQL exception "+e);
        }
        return reply;
    }
    public String getLogin(String[] data){
        String reply=null;
        Boolean check=false;
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            while(rs.next()){
                String id = rs.getString("USERID");
                String token = rs.getString("TOKEN");
                if(id.equals(data[0])&&token.equals(data[1])){
                    log.writeLog("User"+" "+data[0]+" "+"AutoLogin successfull");
                    reply="#*ok*#";
                    rs.afterLast();
                    check=true;
                }
        }
            stmt.close();
            if(check==false){
                log.writeLog("User"+" "+data[0]+" "+"AutoLogin failed");
                    reply="#*failed*#";
            }
            
        }catch (SQLException e) {
             log.writeLog("getLogin SQL exception "+e);
        }
        return reply;//change this
    }
     protected String search(String data){
        String searchdata=data;
        String reply=null;
        log.writeLog("Admin searching for user "+searchdata);
        try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
             while(rs.next()){
                 String id = rs.getString("USERID");
                 if(id.equals(searchdata)){
                    boolean blocked = rs.getBoolean("BLOCKED");
                    String tries = rs.getString("TRIES");
                    reply = "searchResult"+","+id+","+blocked+","+tries;
                    rs.afterLast();
                    log.writeLog(" Admin search for user "+searchdata +"sucessful");
                 }else if(rs.isLast()){
                     reply="notFound";
                     log.writeLog("Admin search for user "+searchdata +" failed");
                 }
             }
            stmt.close();
        }catch (SQLException e) {
             log.writeLog("Search SQL exception "+e);
        }
        return reply;
    }
     public String userUpdate(String[] data){
         String[] userUpdate = data;
         String userUpdateResult =null;
         log.writeLog("User details change requested for user"+" "+userUpdate[1]);
          try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            while(rs.next()){
                String id = rs.getString("USERID");
                if(id.equals(userUpdate[1])){
                    if(userUpdate[2].equals("no_update")){
                        if(userUpdate[3].equals("false")){
                           rs.updateBoolean("BLOCKED", false); 
                        }else if(userUpdate[3].equals("true")){
                            rs.updateBoolean("BLOCKED", true); 
                        }
                        rs.updateInt("TRIES", Integer.parseInt(userUpdate[4]));
                        rs.updateRow();
                        userUpdateResult = "UserUpdated";
                        rs.afterLast();
                        log.writeLog("User credentials updated "+userUpdate[1]);
                    }else{
                        String hashed=null;
                        try{
                            hashed =hash.createHash(userUpdate[2]);
                        }catch(Exception ex){
                            log.writeLog(" "+ ex);
                        }
                        rs.updateString("PASSW0RD", hashed);
                        if(userUpdate[3].equals("false")){
                           rs.updateBoolean("BLOCKED", false); 
                        }else if(userUpdate[3].equals("true")){
                            rs.updateBoolean("BLOCKED", true); 
                        }
                        rs.updateInt("TRIES", Integer.parseInt(userUpdate[4]));
                        rs.updateRow();
                        userUpdateResult = "UserUpdated";
                        rs.afterLast();
                        log.writeLog("User Details updated"+" "+userUpdate[1]);
                    }
                }
            }
              stmt.close();
          }catch (SQLException e){
              log.writeLog("Database error when updating user "+ userUpdate[1] +" "+e);
        }
         return userUpdateResult;
     }//the purpose of this Method is to add new user to the system
     public String addUser(String[] data){
         String[] newUser = data;
         String result=null;
         
         try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL); 
            rs.moveToInsertRow();
            rs.updateString("USERID",newUser[1]);
             String hashed=null;
            try{
              hashed =hash.createHash(newUser[2]); 
            }catch(NoSuchAlgorithmException | InvalidKeySpecException e){
                log.writeLog("Error when hashing password for user "+ newUser[1] +" "+ e);
            }
            rs.updateString("PASSW0RD",hashed);
            rs.updateInt("TRIES", 0);
            rs.updateString("TOKEN","no_token");
            rs.updateBoolean("BLOCKED", false); 
            rs.insertRow();
            stmt.close();
            log.writeLog("New User is being added"+" "+newUser[1]);
            result="userAdded";
            }catch(SQLException e){
                result="unableToAddUser";
                log.writeLog("Database error when adding new user "+ newUser[1] +" "+e);
        }
         return result;
     }
     //This method will delete user from database
     public String removeUser(String[] data){
         String[] user = data;
         String result=null;
         
         try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            while(rs.next()){
                String id = rs.getString("USERID");
                if(id.equals(user[1])){
                    rs.deleteRow();
                    rs.afterLast();
                    result ="removed";
                }
            }
        stmt.close();
        log.writeLog("User "+user[1] +" removed");
        }catch (SQLException e) {
             log.writeLog("Database error when removing user "+user[1]+" "+e);
        }
         return result;
     }
     //This method will delete user from database
     public String showAll(){
         String reply=null;
         try{
            Connection con = DriverManager.getConnection( host, username, password );
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            String SQL = "select * from "+splitter[2];
            ResultSet rs = stmt.executeQuery(SQL);
            reply="allUsers"+",";
            int size=0;
            while(rs.next()){
                size=size+1;
                String id = rs.getString("USERID");
                boolean blocked = rs.getBoolean("BLOCKED");
                String tries = rs.getString("TRIES");
                reply=reply+id+","+blocked+","+tries+",";
            }
            stmt.close();
            reply=reply+size;
            log.writeLog("Details of the all users were provided to admin.");
            }catch (SQLException e) {
             log.writeLog("Error when providing all the user details to the admin "+e);
        }
         
         return reply;
     }
     public boolean confirmUser(String[] data){
         boolean reply=false;
         String[] credentials = data;
         try{
         Connection con = DriverManager.getConnection( host, username, password );
         Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
         String SQL = "select * from "+splitter[2];
         ResultSet rs = stmt.executeQuery(SQL);
          int credentialsLength=credentials.length;
         while(rs.next()){
             String id = rs.getString("USERID");
             if(id.equals(credentials[credentialsLength-2])){
               String token = rs.getString("TOKEN");
               
               if(token.equals(credentials[credentialsLength-1])){
                   reply=true;
                   rs.afterLast();
                   
               }
             }
         }
         stmt.close();
         }catch (SQLException err){
             log.writeLog(" thiserror "+err);
        }
         
         return reply;
     }
}
