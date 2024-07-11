package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
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
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
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
    private static void tearDown() {

    }

    @Test
    public void givenParkingSlotAvailable_whenProcessIncomingCar_thenTicketIsSaved() {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
    }

    @Test
    public void givenParkingSlotAvailable_whenProcessIncomingCar_thenParkingAvailabilityIsUpdated() {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals(1, parkingSpot.getId());
    }

    @Test
    public void givenEverythingOK_whenProcessExitingVehicle_thenTicketIsUpdated() {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // WHEN
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        Date outTime = new Date(ticket.getInTime().getTime() + (60 * 60 * 1000));
        ticket.setOutTime(outTime);
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        fareCalculatorService.calculateFare(ticket);
        ticketDAO.updateTicket(ticket);
        System.out.println("Updating info for the test. Fare: " + ticket.getPrice() + " OutTime: " + ticket.getOutTime());

        // THEN
        assertNotNull(ticket);
        assertNotNull(ticket.getOutTime());
        assertNotEquals(0.0, ticket.getPrice());
    }

    @Test
    public void givenRecurringUser_whenProcessExitingVehicle_thenApplyDiscount() {
        // GIVEN
        // user first visit
        System.out.println("First visit");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket firstTicket = ticketDAO.getTicket("ABCDEF");
        Date firstOutTime = new Date(firstTicket.getInTime().getTime() + (60 * 60 * 1000));
        firstTicket.setOutTime(firstOutTime);
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        fareCalculatorService.calculateFare(firstTicket);
        ticketDAO.updateTicket(firstTicket);
        System.out.println("Please pay the parking fare:" + firstTicket.getPrice());
        System.out.println("Recorded out-time for vehicle number:" + firstTicket.getVehicleRegNumber() + " is:" + firstOutTime);
        Double firstPrice = firstTicket.getPrice();

        // user second entrance
        System.out.println("Second visit");
        ParkingService secondParkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        secondParkingService.processIncomingVehicle();

        // WHEN
        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");
        Date secondOutTime = new Date(secondTicket.getInTime().getTime() + (60 * 60 * 1000));
        secondTicket.setOutTime(secondOutTime);
        secondParkingService.setDiscountForRecurringUser(secondTicket, "ABCDEF");
        fareCalculatorService.calculateFare(secondTicket);
        ticketDAO.updateTicket(secondTicket);
        System.out.println("Please pay the parking fare:" + firstTicket.getPrice());
        System.out.println("Recorded out-time for vehicle number:" + firstTicket.getVehicleRegNumber() + " is:" + firstOutTime);

        // THEN
        Double secondPrice = secondTicket.getPrice();
        System.out.println(firstPrice + " / " + secondPrice );
        assertTrue(secondTicket.isDiscount());
        assertEquals(Math.round(firstPrice * 0.95*100.0)/100.0, secondPrice, 0.01);
    }

}
