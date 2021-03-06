package com.uzapp.rest.admin;

import com.uzapp.bd.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uzapp.dominio.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/users")
public class Users {

    private static final Logger logger = LoggerFactory.getLogger(Users.class);

    private ResponseEntity<?> setUserData(ResultSet rs) {
        try {
            User user = null;
            if(rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setName(rs.getString("name"));
                user.setSurnames(rs.getString("surnames"));
                user.setBirthDate(rs.getDate("birthdate"));
                user.setRole(rs.getString("role"));
            }
            return new ResponseEntity<>(user, HttpStatus.OK);

        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> getUserData(Connection connection, int id, String username){
        logger.info("Method getUserData", id, username);
        try {
            String query = "";
            PreparedStatement preparedStmt;
            if (id != -1) {
                query = "SELECT * FROM users WHERE id=?";
                preparedStmt = connection.prepareStatement(query);
                preparedStmt.setInt(1, id);
            }
            else {
                query = "SELECT * FROM users WHERE username=?";
                preparedStmt = connection.prepareStatement(query);
                preparedStmt.setString(1, username);
            }

            ResultSet rs = preparedStmt.executeQuery();
            ResponseEntity<?> response = setUserData(rs);
            return response;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            try { if (connection != null) connection.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
        }
    }

    @RequestMapping(
            value = "/create",
            method = RequestMethod.POST)
    public ResponseEntity<?> create(@RequestBody User user){
        logger.info("Servicio: create user", user);
        Connection connection = null;
        PreparedStatement preparedStmt = null;
        try {
            connection = ConnectionManager.getConnection();

            String query = "INSERT INTO users(username,password,email,name,surnames,birthDate,role) values (?,?,?,?,?,?,?)";
            preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString(1, user.getUsername());
            preparedStmt.setString(2, user.getPassword());
            preparedStmt.setString(3, user.getEmail());
            preparedStmt.setString(4, user.getName());
            preparedStmt.setString(5, user.getSurnames());
            preparedStmt.setDate(6, user.getBirthDate());
            preparedStmt.setString(7, user.getRole());
            int rowsInserted =preparedStmt.executeUpdate();

            if (rowsInserted > 0) {
                ResponseEntity<?> result = getUserData(connection, -1, user.getUsername());
                return result;
            }
            else {
                return new ResponseEntity<>("Error creating user", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            try { if (preparedStmt != null) preparedStmt.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
            try { if (connection != null) connection.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
        }
    }

    @RequestMapping(
            value = "/login",
            method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody User user){
        logger.info("Servicio: login user");
        Connection connection = null;
        PreparedStatement preparedStmt = null;
        try {
            connection = ConnectionManager.getConnection();

            String query = "SELECT * FROM users WHERE username=? AND password=?";
            preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString(1, user.getUsername());
            preparedStmt.setString(2, user.getPassword());
            ResultSet rs = preparedStmt.executeQuery();

            ResponseEntity<?> response = setUserData(rs);

            if (response.getBody() == null) {
                return new ResponseEntity<>("Username/password invalids, user not found", HttpStatus.NOT_FOUND);
            }
            else {
                return response;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            try { if (preparedStmt != null) preparedStmt.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
            try { if (connection != null) connection.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
        }
    }

    @RequestMapping(
            value = "/edit",
            method = RequestMethod.PUT)
    public ResponseEntity<?> edit(@RequestBody User user){
        logger.info("Servicio: edit user");
        Connection connection = null;
        PreparedStatement preparedStmt = null;
        try {
            connection = ConnectionManager.getConnection();
            String password = user.getPassword();
            String query = "";
            if (password == null) {
                query = "UPDATE users SET username=?, email=?, name=?, surnames=?, birthdate=?, role=? where id=?";
            } else {
                query = "UPDATE users SET username=?, email=?, name=?, surnames=?, birthdate=?, role=?, password=? where id=?";
            }

            System.out.print("password: " + password);

            preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString(1, user.getUsername());
            preparedStmt.setString(2, user.getEmail());
            preparedStmt.setString(3, user.getName());
            preparedStmt.setString(4, user.getSurnames());
            preparedStmt.setDate(5, user.getBirthDate());
            preparedStmt.setString(6, user.getRole());
            if (password == null) {
                preparedStmt.setInt(7, user.getId());
            } else {
                preparedStmt.setString(7, password);
                preparedStmt.setInt(8, user.getId());
            }
            int rowsUpdated = preparedStmt.executeUpdate();
            System.out.println("rowsUpdated on edit: " + rowsUpdated);

            if (rowsUpdated > 0) {
                ResponseEntity<?> result = getUserData(connection, -1, user.getUsername());
                return new ResponseEntity<>(result, HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            try { if (preparedStmt != null) preparedStmt.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
            try { if (connection != null) connection.close(); }
            catch (Exception excep) { excep.printStackTrace(); }
        }
    }

    @RequestMapping(
            value = "/**",
            method = RequestMethod.OPTIONS
    )
    public ResponseEntity handle() {
        return new ResponseEntity(HttpStatus.OK);
    }
}
