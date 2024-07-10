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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void givenParkingSlotAvailable_whenProcessIncomingCar_thenTicketIsSaved(){
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
    }

    @Test
    public void givenParkingSlotAvailable_whenProcessIncomingCar_thenParkingAvailabilityIsUpdated(){
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals( 1, parkingSpot.getId() );
    }

    @Test
    public void givenEverythingOK_whenProcessExitingVehicle_thenTicketIsUpdated() {
        // GIVEN
        givenParkingSlotAvailable_whenProcessIncomingCar_thenTicketIsSaved();
        givenParkingSlotAvailable_whenProcessIncomingCar_thenParkingAvailabilityIsUpdated();
        ParkingService parkingService = new ParkingService( inputReaderUtil, parkingSpotDAO, ticketDAO );
        Ticket ticket = ticketDAO.getTicket( "ABCDEF" );
        Date outTime = new Date(ticket.getInTime().getTime() + ( 35 * 60 * 1000 ));
        ticket.setOutTime( outTime );

        // WHEN
        ticketDAO.updateTicket( ticket );
        parkingService.processExitingVehicle();

        // THEN
        assertNotNull(ticket);
        assertNotNull(ticket.getOutTime());
        assertNotEquals(0, ticket.getPrice());
    }

    @Test
    public void givenRecurringUser_whenProcessExitingVehicle_thenApplyDiscount() {
        // GIVEN
        ParkingService firstParkingService = new ParkingService( inputReaderUtil, parkingSpotDAO, ticketDAO );

        // first User Visit
        firstParkingService.processIncomingVehicle();
        Ticket firstTicket = ticketDAO.getTicket("ABCDEF");

        Date firstOutTime = new Date(firstTicket.getInTime().getTime() + ( 35 * 60 * 1000 ));
        firstTicket.setOutTime( firstOutTime );
        ticketDAO.updateTicket( firstTicket );
        firstParkingService.processExitingVehicle();
        Double firstPrice = firstTicket.getPrice();


        // second User visit
        ParkingService secondParkingService = new ParkingService( inputReaderUtil, parkingSpotDAO, ticketDAO );

        secondParkingService.processIncomingVehicle();
        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");
        Date secondOutTime = new Date(secondTicket.getInTime().getTime() + ( 35 * 60 * 1000 ));
        secondTicket.setOutTime( secondOutTime );
        ticketDAO.updateTicket( secondTicket );

        // WHEN
        secondParkingService.processExitingVehicle();

        // THEN
        assertTrue( secondTicket.isDiscount());
        Double secondPrice = secondTicket.getPrice();
        assertEquals( firstPrice * 0.95, secondPrice, 0.01 );
    }

}
