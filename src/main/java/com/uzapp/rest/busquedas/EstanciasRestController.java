package com.uzapp.rest.busquedas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.google.gson.Gson;
import com.uzapp.bd.ConnectionManager;
import com.uzapp.dominio.Campus;
import com.uzapp.dominio.Edificio;
import com.uzapp.dominio.Espacios;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/estancias")
public class EstanciasRestController {
	
	private static final Logger logger = LoggerFactory.getLogger(EstanciasRestController.class);
	
	public ResultSet getRoomInfo(Connection connection, String roomId) throws SQLException 
	{
		logger.info("Method: getRoomInfo()");

		String querySelect = "SELECT DISTINCT \"TBES\".\"ID_ESPACIO\", \"TBES\".\"ID_CENTRO\", ";
		querySelect += "\"TBED\".\"EDIFICIO\" AS \"edificio\", \"TBED\".\"DIRECCION\" AS \"dir\", ";
		querySelect += "\"TBCI\".\"CIUDADES\" AS \"ciudad\", \"TBCC\".\"CAMPUS\" AS \"campus\" ";

		String queryFrom = "FROM \"TB_ESPACIOS\" \"TBES\", \"TB_CIUDADES\" \"TBCI\", ";
		queryFrom += "\"TB_EDIFICIOS\" \"TBED\", \"TB_CODIGOS_DE_CAMPUS\" \"TBCC\" ";

		String queryWhere = "WHERE \"TBES\".\"ID_ESPACIO\" = ? AND \"TBES\".\"ID_EDIFICIO\"=\"TBED\".\"ID_EDIFICIO\" ";
		queryWhere +=  "AND \"TBED\".\"CAMPUS\"=\"TBCC\".\"ID\" AND \"TBCC\".\"CIUDAD\"=\"TBCI\".\"ID\"";

		String query = querySelect + queryFrom + queryWhere;
	
		PreparedStatement preparedStmt = connection.prepareStatement(query);
		preparedStmt.setString(1, roomId.trim());
		ResultSet res = preparedStmt.executeQuery();

		return res;
	}

	@RequestMapping(
			value = "/id_estancia", 
			method = RequestMethod.GET,
			produces = "application/json")
	public ResponseEntity<?> roomInfo(@RequestParam("estancia") String estancia)
	{
		logger.info("Service: roomInfo()");
		Connection connection = null;
		Espacios infoResult = null;
		try {
			connection = ConnectionManager.getConnection();
			Gson gson = new Gson();

			ResultSet respuesta = getRoomInfo(connection, estancia);
			if (respuesta.next()){
				infoResult = new Espacios(
												respuesta.getString("ID_ESPACIO"),
												respuesta.getString("ID_CENTRO"),
												respuesta.getString("edificio"),
												respuesta.getString("dir"),
												respuesta.getString("ciudad"),
												respuesta.getString("campus"));
			}

			System.out.println("Room info result: "+gson.toJson(infoResult));
			return new ResponseEntity<>(gson.toJson(infoResult), HttpStatus.OK);	
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
			value = "/getEstancia", 
			method = RequestMethod.GET,
			produces = "application/json")
	public ResponseEntity<?> getRoomDetails(@RequestParam("estancia") String estancia)
	{
		logger.info("Service: getRoomDetails()");
		Connection connection = null;
		PreparedStatement preparedStmt = null;
		try {
			connection = ConnectionManager.getConnection();
			Gson gson = new Gson();

			String query = "SELECT DISTINCT \"TBES\".\"ID_ESPACIO\" ,\"TBES\".\"ID_CENTRO\", \"TBUSO\".\"TIPO_DE_USO\", round(\"TBES\".\"SUPERFICIE\",2) AS \"SUPERFICIE\" ";
			query += "FROM \"TB_ESPACIOS\" \"TBES\",\"TB_TIPO_DE_USO\" \"TBUSO\"  ";
			query += "WHERE \"TBES\".\"TIPO_DE_USO\" = \"TBUSO\".ID AND \"TBES\".\"ID_ESPACIO\" = ?";

			Espacios result;
			preparedStmt = connection.prepareStatement(query);
			preparedStmt.setString(1, estancia);
			ResultSet res = preparedStmt.executeQuery();

			if (res.next()){
				result=new Espacios(res.getString("ID_ESPACIO"),res.getString("ID_CENTRO"),res.getString("TIPO_DE_USO"),res.getString("SUPERFICIE"));
				System.out.println("Room details result: "+gson.toJson(result));
      	return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
			}
			else {
				return new ResponseEntity<>("", HttpStatus.OK);
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
			value = "/getAllEstancias", 
			method = RequestMethod.GET,
			produces = "application/json")
	public ResponseEntity<?> getAllRooms(@RequestParam("estancia") String espacio)
	{
		logger.info("Service: getAllRooms()");
		Connection connection = null;
		PreparedStatement preparedStmt = null;
		try {
			connection = ConnectionManager.getConnection();
			Gson gson = new Gson();

			String query = "SELECT DISTINCT \"ID_ESPACIO\" ,\"ID_CENTRO\" ";
			query += "FROM \"TB_ESPACIOS\" ";
			query += "WHERE \"ID_ESPACIO\" LIKE ? ORDER BY \"ID_CENTRO\" ASC";

			List<Espacios> roomsResult = new ArrayList<Espacios>();
			
			preparedStmt = connection.prepareStatement(query);
			preparedStmt.setString(1, espacio+"%");
			System.out.println(preparedStmt);
			ResultSet res = preparedStmt.executeQuery();

			while (res.next()){
				roomsResult.add(new Espacios(res.getString("ID_ESPACIO"),res.getString("ID_CENTRO")));
			}
			
      return new ResponseEntity<>(gson.toJson(roomsResult), HttpStatus.OK);
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
			value = "/campus", 
			method = RequestMethod.GET,
			produces = "application/json")
	public ResponseEntity<?> getCampusValues()
	{
		logger.info("Service: getCampusValues()");
		Connection connection = null;
		try {
			connection = ConnectionManager.getConnection();
			Gson gson = new Gson();

			String query = "SELECT \"CAMPUS\" FROM \"TB_CODIGOS_DE_CAMPUS\"";
			System.out.println(query);

			List<String> result = new ArrayList<String>();
			ResultSet res = connection.prepareStatement(query).executeQuery();

			while (res.next()){
				result.add(res.getString("CAMPUS"));
			}
			
			connection.close();
      return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
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
}
