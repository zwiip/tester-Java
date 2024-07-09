package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;
    private Ticket ticket;
    private ParkingSpot parkingSpot;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
            ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void givenAvailableParkingSpot_whenProcessIncomingCar_thenSaveTicket() {
        // GIVEN
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(inputReaderUtil.readSelection()).thenReturn(1); // Suppose a CAR is selected
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void givenAvailableParkingSpot_whenProcessIncomingBike_thenSaveTicket() {
        // GIVEN
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(inputReaderUtil.readSelection()).thenReturn(2); // Suppose a BIKE is selected
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void givenFullParking_whenProcessIncomingVehicle_thenNoTicketSaved() {
        // GIVEN
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
        when(inputReaderUtil.readSelection()).thenReturn(1); // Suppose a CAR is selected

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(ticketDAO, times(0)).saveTicket(any(Ticket.class));
    }

    @Test
    public void whenProcessExitingVehicle_thenUpdateOutTime() throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        assertNotNull(ticket.getOutTime() );
    }

    @Test
    public void givenUpdateTicketTrue_whenProcessExitingVehicle_thenUpdateParkingSpot()  throws Exception  {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify( parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class) );
        assertTrue( parkingSpot.isAvailable() );
    }

    @Test
    public void givenUpdateTicketFalse_whenProcessExitingVehicle_thenUnableUpdate() throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void givenMultipleTickets_whenProcessExitingVehicle_thenApplyDiscount()  throws Exception  {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when( ticketDAO.getNumberTicket( "ABCDEF" ) ).thenReturn(1);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify( ticketDAO ).getNumberTicket( "ABCDEF" );
        assertTrue( ticket.isDiscount() );
    }

    @Test
    public void givenOneTicket_whenProcessExitingVehicle_thenNoDiscount()  throws Exception  {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when( ticketDAO.getNumberTicket( "ABCDEF" ) ).thenReturn(0);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify( ticketDAO ).getNumberTicket( "ABCDEF" );
        assertFalse( ticket.isDiscount() );
    }

    @Test
    public void givenParkingType_whenGetNextParkingNumberIfAvailable_thenReturnSpotId1() throws Exception {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1); // Suppose a CAR is selected
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNotNull(parkingSpot);
        assertEquals(1, parkingSpot.getId());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertTrue(parkingSpot.isAvailable());
    }

    @Test
    public void givenParkingType_whenGetNextParkingNumberIfAvailableParkingNumberNotFound_thenReturnNull() throws Exception {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1); // Suppose a CAR is selected
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot);
    }

    @Test
    public void givenInvalidVehicleType_whenGetNextParkingNumberIfAvailableParkingNumberWrongArgument_thenReturnNull() throws Exception {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3); // Invalid type

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot);
    }

}
