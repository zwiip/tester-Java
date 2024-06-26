package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException( "Out time provided is incorrect:"+ticket.getOutTime().toString() );
        }

        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();

        double duration = ( double )( outTime - inTime ) / ( 1000 * 60 * 60 );

        if ( ( duration * 60 ) < 30 ) {
            ticket.setPrice( duration * 0 );
        } else {
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }
            if (ticket.isDiscount()) {
                double price = ticket.getPrice();
                ticket.setPrice( price * 0.95 );
            }
        }
    }
}