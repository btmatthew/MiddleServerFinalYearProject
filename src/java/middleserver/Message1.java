package middleserver;

import java.io.Serializable;

/**
 *
 * @author Matthew Bulat
 */
public class Message1 implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String request;
	private String data;
        private String email;
        private String token;
        
        protected String readRequest(){
            return this.request;
        }
        protected String readData(){
            return this.data;
        }
        protected String readToken(){
            return this.token;
        }
        protected String readEmail(){
            return this.email;
        }
        protected void setToken(String data){
            this.token=data;
        }
        protected void setData(String data){
            this.data=data;
        }
        protected void setRequest(String data){
            this.request=data;
        }
        protected void setEmail(String data){
		this.email=data;
	}
	protected String getAutoEmail(){
            return data;
        }
	protected byte[] getBytes(){
		String stringed=request +"!"+data+"!"+token;
		return stringed.getBytes(); 
	}
	protected String getRequest(byte[] data1){
                String getText = new String(data1);
		String[] splitter=getText.split("!");
		request = splitter[0];
		return request;
		
	}
        protected String getEmail(byte[] data1){
                String getText = new String(data1);
		String[] splitter=getText.split("!");
		email = splitter[2];
		return email;
	}
	protected String getData(byte[] data1){
		String getText = new String(data1);
		String[] splitter=getText.split("!");
		data = splitter[1];
		return data;
		
	}
        protected String getAutoToken(byte[] data1){
		String getText = new String(data1);
		String[] splitter=getText.split("!");
		data = splitter[2];
		return data;
	}
	protected String getToken(byte[] data1){
		String getText = new String(data1);
		String[] splitter=getText.split("!");
		data = splitter[3];
		return data;
	}

}
