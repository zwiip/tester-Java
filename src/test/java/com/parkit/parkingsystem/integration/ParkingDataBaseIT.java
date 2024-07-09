package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    /**
     * check that a ticket is actualy saved in DB and Parking table is updated with availability
     */
    @Test
    public void testParkingACar(){
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals( 1, parkingSpot.getId() );
    }

    @Test
    public void testParkingLotExit() throws Exception {
        // GIVEN
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket ticket = ticketDAO.getTicket( "ABCDEF" );
        Date outTime = new Date(ticket.getInTime().getTime() + (35 * 60 * 1000));
        ticket.setOutTime( outTime );

        // WHEN
        ticketDAO.updateTicket( ticket );
        parkingService.processExitingVehicle();

        // THEN
        assertNotNull(ticket);
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(updatedTicket.getOutTime());
        assertNotEquals(0, updatedTicket.getPrice());
        //TODO: check that the fare generated and out time are populated correctly in the database
    }

}
