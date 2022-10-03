package tableClass;

import java.io.Serializable;

//사용자 테이블
public class User implements Serializable
{
	private static final long serialVersionUID = 2L;
	
	private String userID;				//사용자 ID
	private String nickName;			//닉네임
	private String password;			//password
	private int separaterUser;		//사용자 구분 1, 2
	private String address;				//주소
	
	public User()
	{
		userID =null; nickName = null; password =null; separaterUser =0; address =null;
	}
	
	public User(String userID, String nickName, String password, int separaterUser, String address)
	{
		this.userID= userID; this.nickName = nickName; this.password = password; 
		this.separaterUser = separaterUser; this.address = address;
	}

	public User(String userID, String password)
	{
		this.userID= userID; this.password = password;
		nickName = null; separaterUser =0; address =null;
	}
	
	public String getUserID() {return userID;}
	public String getNickName() {return nickName;}
	public String getPassword() {return password;}
	public int getSeparaterUser() {return separaterUser;}
	public String getAddress() {return address;}
	
	public void setUserID(String userID) {this.userID = userID;}
	public void setNickName(String nickName) {this.nickName = nickName;}
	public void setPassword(String password) {this.password = password;}
	public void setSeparaterUser(int separaterUser) {this.separaterUser = separaterUser;}
	public void setAddress(String address) {this.address = address;} 
}
