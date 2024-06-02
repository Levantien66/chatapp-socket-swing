package com.raven.service;

import com.raven.connection.DatabaseConnection;
import com.raven.model.Model_Client;
import com.raven.model.Model_Login;
import com.raven.model.Model_Message;
import com.raven.model.Model_Register;
import com.raven.model.Model_User_Account;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {
    private final Connection con;

    public ServiceUser() {
        this.con = DatabaseConnection.getInstance().getConnection();
    }

    public Model_Message register(Model_Register data) {
        Model_Message message = new Model_Message();
        try {
            // Kiểm tra xem người dùng đã tồn tại chưa
            try (PreparedStatement p = con.prepareStatement(CHECK_USER)) {
                p.setString(1, data.getUserName());
                try (ResultSet r = p.executeQuery()) {
                    if (r.first()) {
                        message.setAction(false);
                        message.setMessage("User Already Exists");
                    } else {
                        message.setAction(true);
                    }
                }
            }

            if (message.isAction()) {
                // Thêm người dùng mới
                con.setAutoCommit(false);
                int userID;
                try (PreparedStatement p = con.prepareStatement(INSERT_USER, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, data.getUserName());
                    p.setString(2, data.getPassword()); // Nên mã hóa mật khẩu ở đây
                    p.executeUpdate();
                    try (ResultSet r = p.getGeneratedKeys()) {
                        if (r.first()) {
                            userID = r.getInt(1);
                        } else {
                            throw new SQLException("User ID generation failed");
                        }
                    }
                }

                // Tạo tài khoản người dùng
                try (PreparedStatement p = con.prepareStatement(INSERT_USER_ACCOUNT)) {
                    p.setInt(1, userID);
                    p.setString(2, data.getUserName());
                    p.executeUpdate();
                }

                con.commit();
                con.setAutoCommit(true);
                message.setAction(true);
                message.setMessage("Ok");
                message.setData(new Model_User_Account(userID, data.getUserName(), "", "", true));
            }
        } catch (SQLException e) {
            message.setAction(false);
            message.setMessage("Server Error");
            try {
                if (!con.getAutoCommit()) {
                    con.rollback();
                    con.setAutoCommit(true);
                }
            } catch (SQLException e1) {
                // Log lỗi nếu cần thiết
            }
        }
        return message;
    }

    public Model_User_Account login(Model_Login login) throws SQLException {
        Model_User_Account data = null;
        try (PreparedStatement p = con.prepareStatement(LOGIN)) {
            p.setString(1, login.getUserName());
            p.setString(2, login.getPassword());
            try (ResultSet r = p.executeQuery()) {
                if (r.first()) {
                    int userID = r.getInt(1);
                    String userName = r.getString(2);
                    String gender = r.getString(3);
                    String image = r.getString(4);
                    data = new Model_User_Account(userID, userName, gender, image, true);
                }
            }
        }
        return data;
    }

    public List<Model_User_Account> getUser(int exitUser) throws SQLException {
        List<Model_User_Account> list = new ArrayList<>();
        try (PreparedStatement p = con.prepareStatement(SELECT_USER_ACCOUNT)) {
            p.setInt(1, exitUser);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int userID = r.getInt(1);
                    String userName = r.getString(2);
                    String gender = r.getString(3);
                    String image = r.getString(4);
                    list.add(new Model_User_Account(userID, userName, gender, image, checkUserStatus(userID)));
                }
            }
        }
        return list;
    }

    private boolean checkUserStatus(int userID) {
        List<Model_Client> clients = Service.getInstance(null).getListClient();
        for (Model_Client c : clients) {
            if (c.getUser().getUserID() == userID) {
                return true;
            }
        }
        return false;
    }

    // Các câu lệnh SQL
    private static final String LOGIN = "SELECT UserID, user_account.UserName, Gender, ImageString FROM `user` JOIN user_account USING (UserID) WHERE `user`.UserName=BINARY(?) AND `user`.`Password`=BINARY(?) AND user_account.`Status`='1'";
    private static final String SELECT_USER_ACCOUNT = "SELECT UserID, UserName, Gender, ImageString FROM user_account WHERE user_account.`Status`='1' AND UserID<>?";
    private static final String INSERT_USER = "INSERT INTO user (UserName, `Password`) VALUES (?,?)";
    private static final String INSERT_USER_ACCOUNT = "INSERT INTO user_account (UserID, UserName) VALUES (?,?)";
    private static final String CHECK_USER = "SELECT UserID FROM user WHERE UserName =? LIMIT 1";
}