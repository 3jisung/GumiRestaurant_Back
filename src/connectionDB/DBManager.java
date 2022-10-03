package connectionDB;

import tableClass.*;
import Network.*;

import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;


//import com.mysql.cj.xdevapi.Statement;
// jdbc:mysql://192.168.209.250:3306/dorm
// 카페 192.168.209.250
public class DBManager {
	public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	public static final String URL = "jdbc:mysql://" + "localhost" + ":3306" + "/myproject" + "?characterEncoding=UTF-8&serverTimezone=UTC";

	private Connection conn = null;
	private PreparedStatement pstmt = null;
	private Statement stmt = null;
	private ResultSet rs = null;
	
	private User currentUser;	// 클라이언트의 사용자 정보를 담는 객체

	public DBManager(String id, String pw)   //생성자
	{    
		try{
			// 1. 드라이버 로딩
			// 드라이버 인터페이스를 구현한 클래스를 로딩
			// mysql, oracle 등 각 벤더사 마다 클래스 이름이 다르다.
			// mysql은 "com.mysql.jdbc.Driver"이며, 이는 외우는 것이 아니라 구글링하면 된다.
			// 참고로 이전에 연동했던 jar 파일을 보면 com.mysql.jdbc 패키지에 Driver 라는 클래스가 있다.
			//         String dbID, dbPW;
			//         Scanner scan = new Scanner(System.in);
			//         
			//         System.out.print("db manager id :");
			//         dbID = scan.nextLine();
			//         System.out.print("db manager password :");
			//         dbPW = scan.nextLine();      //db manager 생성시 id와 pw를 입력받아서 원격으로 db에 로그인한다

			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(URL, id, pw);
			stmt = conn.createStatement();

		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public void closeConnection() throws SQLException
	{
		try {
			//자원 반환
			conn.close();
		}catch(Exception e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("자원 반환");
		}
	}
	////////////////////////////////////////////////////////////////////////////
	//기능
	
	public void loginCheck(Protocol protocol, User user) throws SQLException	//로그인
	{
		String loginId = user.getUserID();
		String loginPw = user.getPassword();

		String SQL = "SELECT * FROM myproject.사용자";
		rs = stmt.executeQuery(SQL);
		//사용자 테이블의 모든 ID 검색 혹은 일치하는 ID가 있다면 PW 일치 확인 
		while(rs.next()) {    
			if (loginId.equals(rs.getString("사용자id"))) 
			{   //아이디가 맞는 경우
				if (loginPw.equals(rs.getString("비밀번호")))
				{
					System.out.println("로그인성공");
					currentUser = user;
					currentUser.setNickName(rs.getString("닉네임"));
					currentUser.setSeparaterUser(rs.getInt("사용자구분"));
					currentUser.setAddress(rs.getString("주소"));

					if(rs.getString("사용자구분").equals("1") == true)		//사용자가 학생인 경우 학번(id)로 학생테이블에서 학생 정보를 객체에 담아서 클라이언트로 보낸다
					{
						protocol.makePacket(1,2,1, currentUser);		//클라이언트로 보낼 protocol setting
						return;
					}

					else		//사용자가 관리자나 담당직원인 경우 이름을 클라이언트로 보낸다
					{
						protocol.makePacket(1,2,2, currentUser);	
						return;
					}
				} 
			}
		}
		protocol.makePacket(1,2,3, "해당정보 없음");
	}
	
	
	public void checkID(Protocol protocol)		//아이디 중복검사
	{
		String id = (String)protocol.getBody();
		
		String sql1 = "SELECT 사용자id FROM myproject.사용자 where 사용자id=\"" + id + "\"";
		
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			
			if(rs.getRow() == 0)
			{
				protocol.makePacket(2, 2, 1, null);
			}
			else
				protocol.makePacket(2, 2, 2, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(2, 2, 3, "중복검사가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void checkNick(Protocol protocol)	//닉네임 중복검사
	{
		String nick = (String)protocol.getBody();
		
		String sql1 = "SELECT 닉네임 FROM myproject.사용자 where 닉네임=\"" + nick + "\"";
		
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			
			if(rs.getRow() == 0)
			{
				protocol.makePacket(3, 2, 1, null);
			}
			else
				protocol.makePacket(3, 2, 2, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(3, 2, 3, "중복검사가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void createCustomer(Protocol protocol)	//회원가입
	{
		User user = (User)protocol.getBody();
		
		try {
			String sql1 = "INSERT INTO myproject.사용자 (사용자id, 닉네임, 비밀번호, 사용자구분, 주소)"
					+ " VALUES (\"" + user.getUserID() + "\", \"" + user.getNickName() + "\", \""
					+ user.getPassword() + "\", " + user.getSeparaterUser() + ", \"" + user.getAddress() + "\")";
			stmt.executeUpdate(sql1);
				
			protocol.makePacket(4, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(4, 2, 2, "회원가입이 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void printRestaurants(Protocol protocol)	//음식점 목록 출력
	{
		String sql1 = "select * from myproject.음식점";
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			int arr_low = rs.getRow();
			Restaurant[] res = new Restaurant[arr_low];
			rs.beforeFirst();	//rs 커서를 검색된 테이블의 맨 처음 행으로 돌림
//			System.out.println(cnt);
			int i = 0;
			while(rs.next())	//각 행을 탐색하면서 배열에 값을 채운다 arr[row][속성값]
			{
				res[i] = new Restaurant();
				res[i].setRestaurantID(rs.getInt("음식점id"));
				res[i].setRestaurantName(rs.getString("업소명"));
				res[i].setAddress(rs.getString("주소"));
				res[i].setDistrict(rs.getString("행정구역")); 
				res[i].setContactAddress(rs.getString("연락처"));
				res[i].setMainDish(rs.getString("주된메뉴"));
				res[i].setAverStarPoint(rs.getDouble("평균별점"));
				i++;
			}
			int j = 0;
			while(j < arr_low)
			{
				String sql2 = "SELECT * FROM myproject.리뷰 where 음식점id=" + res[j].getRestaurantID();
				rs = stmt.executeQuery(sql2);
				rs.last();
				res[j].setRevAmount(rs.getRow());
				j++;
			}	
			protocol.makePacket(11, 2, 1, res);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(11, 2, 2, "조회가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void printReview(Protocol protocol)	//해당 음식점 리뷰 출력
	{
		Restaurant res = (Restaurant)protocol.getBody();
		String sql1 = "SELECT * FROM myproject.리뷰 where 음식점id=" + res.getRestaurantID();
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			int arr_low = rs.getRow();
			String[][] rev = new String[arr_low][4];
			rs.beforeFirst();	//rs 커서를 검색된 테이블의 맨 처음 행으로 돌림
			int i = 0;
			while(rs.next())	//각 행을 탐색하면서 배열에 값을 채운다 arr[row][속성값]
			{
				rev[i][0] = rs.getString("사용자id");
				rev[i][1] = rs.getString("내용");
				rev[i][2] = Double.toString(rs.getDouble("별점"));
				rev[i][3] = Integer.toString(rs.getInt("리뷰번호"));
				i++;
			}
			
			int j = 0;
			while(j < arr_low)
			{
				String sql2 = "SELECT 닉네임 FROM myproject.사용자 where 사용자id=\"" + rev[j][0] + "\"";
				rs = stmt.executeQuery(sql2);
				rs.next();
				rev[j][0] = rs.getString("닉네임");
				j++;
			}			
			
			protocol.makePacket(12, 2, 1, rev);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(12, 2, 2, "조회가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void createReview(Protocol protocol)		//리뷰 등록
	{
		Review rev = (Review)protocol.getBody();
		
		try {
			String preQuery = "INSERT INTO myproject.리뷰(사용자id,음식점id,별점,내용) VALUES(?,?,?,?)";
			pstmt = conn.prepareStatement(preQuery); 
			pstmt.setString(1, rev.getUserID());
			pstmt.setInt(2, rev.getRestaurantID());
			pstmt.setDouble(3, rev.getStarPoint());
			pstmt.setString(4, rev.getContent());
			pstmt.executeUpdate();
				
			protocol.makePacket(13, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(13, 2, 2, "리뷰 등록이 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void updateStarPoint(Protocol protocol)		//평균 별점 최신화
	{
		Restaurant res = (Restaurant)protocol.getBody();
		
		try {
			String sql1 = "Select 별점 from myproject.리뷰 where 음식점id=" + res.getRestaurantID();
			rs = stmt.executeQuery(sql1);
			
			Double sp = 0.0;
			
			while(rs.next()) { sp += rs.getDouble("별점"); }
			
			rs.last();
			
			if(rs.getRow() != 0)
				sp /= rs.getRow();
			
			String sql2 = "Update myproject.음식점 set 평균별점=" + sp + " where 음식점id=" + res.getRestaurantID();
			stmt.executeUpdate(sql2);
			
			res.setAverStarPoint(sp);
			res.setRevAmount(res.getRevAmount() + 1);
				
			protocol.makePacket(14, 2, 1, res);
//			protocol.makePacket(14, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(14, 2, 2, "별점 최신화가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void updateNickName(Protocol protocol)	//닉네임 변경
	{
		User user = (User)protocol.getBody();
		
		String sql1 = "SELECT 닉네임 FROM myproject.사용자 where 닉네임=\"" + user.getNickName() + "\"";
		
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			
			if(rs.getRow() == 0)
			{
				String sql2 = "UPDATE myproject.사용자 SET 닉네임=\"" + user.getNickName() + "\" where 사용자id=\"" + user.getUserID() + "\"";
				stmt.executeUpdate(sql2);
				
				protocol.makePacket(15, 2, 1, null);
			}
			else
				protocol.makePacket(15, 2, 2, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(15, 2, 3, "변경이 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void updateAddress(Protocol protocol)	//주소 변경
	{
		User user = (User)protocol.getBody();
		
		try {
			String sql1 = "UPDATE myproject.사용자 SET 주소=\"" + user.getAddress() + "\" where 사용자id=\"" + user.getUserID() + "\"";
			stmt.executeUpdate(sql1);
				
			protocol.makePacket(16, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(16, 2, 2, "변경이 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void updatePassword(Protocol protocol)	//비밀번호 변경
	{
		User user = (User)protocol.getBody();
		
		try {
			String sql1 = "UPDATE myproject.사용자 SET 비밀번호=\"" + user.getPassword() + "\" where 사용자id=\"" + user.getUserID() + "\"";
			stmt.executeUpdate(sql1);
				
			protocol.makePacket(17, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(17, 2, 2, "변경이 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void printMyReview(Protocol protocol)	//내 리뷰 보기
	{
		String id = (String)protocol.getBody();
		
		String sql1 = "SELECT * FROM myproject.리뷰 where 사용자id=\"" + id + "\"";
		
		try {
			rs = stmt.executeQuery(sql1);
			rs.last();
			
			int arr_len = rs.getRow();
			rs.beforeFirst();
			
			String[][] rev = new String[arr_len][5];
			
			int i = 0;
			while(rs.next())
			{
				rev[i][1] = rs.getString("내용");
				rev[i][2] = Double.toString(rs.getDouble("별점"));
				rev[i][3] = Integer.toString(rs.getInt("리뷰번호"));
				rev[i][4] = Integer.toString(rs.getInt("음식점id"));
				
				i++;
			}
			
			int j = 0;
			while(j < rev.length)
			{
				String sql2 = "SELECT 업소명 FROM myproject.음식점 where 음식점id=\"" + rev[j][4] + "\"";
				rs = stmt.executeQuery(sql2);				
				rs.next();			
				rev[j][0] = rs.getString("업소명");			
				j++;
			}

			protocol.makePacket(21, 2, 1, rev);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(21, 2, 2, "내 리뷰를 정상적으로 불러오지 못했습니다.");
		}
	}
	
	
	public void deleteReview(Protocol protocol)	//리뷰 삭제
	{
		int revNum = (int)protocol.getBody();
		
		try {
			String sql1 = "Delete from myproject.리뷰 where 리뷰번호=" + revNum;
			stmt.executeUpdate(sql1);
				
			protocol.makePacket(22, 2, 1, null);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			protocol.makePacket(22, 2, 2, "리뷰 삭제가 정상적으로 이루어지지 않았습니다.");
		}
	}
	
	
	public void updateDBtable(Protocol protocol)
	{
//		try {
//			
//				
//			protocol.makePacket(31, 2, 1, null);
//			
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			protocol.makePacket(31, 2, 2, "DB 최신화가 정상적으로 이루어지지 않았습니다.");
//		}
	}
	
	
	
	
	
	

	private static void checkWarnings(SQLWarning w) throws SQLException {
		if (w != null) {
			while (w != null) {
				System.out.println("SQL 상태:" + w.getSQLState());
				System.out.println("자바 예외 메세지:" + w.getMessage());
				System.out.println("DBMS 에러 코드:" + w.getErrorCode());
				w.getNextWarning();
			}
		}
	}
}