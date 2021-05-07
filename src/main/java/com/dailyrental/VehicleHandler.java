/*
 * File: VehicleHandler
 * Created on 09.09.2020
 *
 * Copyright (c) 2020 by Daimler AG
 */
package com.dailyrental;

import com.dailyrental.domain.Rental;
import com.dailyrental.domain.VehicleMovement;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.dailyrental.rule.CheckoutRule.*;
import static com.dailyrental.rule.RentalRule.isPrivateRental;

/**
 * @author Michael Grote, GROTEMI (415)
 */
@Component
public class VehicleHandler {
    @Autowired
    private RentalService rentalService;

    /**
     * There are two types of rental use cases with different rules:
     * RentalType.GARAGE_RENTAL for business users
     * RentalType.PRIVAT_RENTAL for private users
     * The following rules apply:
     * GARAGE_RENTAL:
     * In case the checkout happens more than 2 hours early the planned rental end time will be exactly 2 hours earlier (2minusRule).
     * In case the checkout happens more than 4 hours early the planned rental end time will be exactly 4 hours earlier (4minusRule).
     * In case the checkout happens more than 2 hours later than planned, the planned rental end time will be exactly 2 hours later (2plusRule).
     * In case the checkout happens more than 4 hours later than planned, the planned rental end time will be exactly 4 hours later (4plusRule).
     * In case the customer is marked as a favorite, none of the rules above will apply.
     * PRIVATE_RENTAL:
     * In case the checkout happens early, the rental end will be set to an earlier time according
     * to the difference between checkout and expected start time rounded up to full hours.
     * In case the customer is marked as a favorite, the rule above does not apply.
     *
     * @param rental       Rental with information about time ranges and customer
     * @param checkoutDate the real vehicle checkout time
     */
    public void checkout(Rental rental, LocalDateTime checkoutDate) {
        var movement = new VehicleMovement();
        movement.setCheckout(checkoutDate);
        rental.setMovement(movement);
        if(isEarly.and(isNotFavoriteCustomer).test(rental, checkoutDate)) {
            applyCheckoutRules(rental, checkoutDate);
        }
        rentalService.updateRental(rental);
    }

    private void applyCheckoutRules(Rental rental, LocalDateTime checkoutDate) {
        if(isPrivateRental.test(rental)) {
            rental.setEnd(rental.getEnd().minusHours((long) rental.getStart().getHour() - checkoutDate.getHour()));
        } else {
            applyGarageCheckoutRules(rental, checkoutDate);
        }
    }

    private void applyGarageCheckoutRules(Rental rental, LocalDateTime checkoutDate) {
        if(fourMinusRuleApplies.test(rental, checkoutDate)) {
            rental.setEnd(rental.getEnd().minusHours(4));
        } else if (twoMinusRuleApplies.test(rental, checkoutDate)) {
            rental.setEnd(rental.getEnd().minusHours(2));
        }
    }

    public void checkin(Rental rental) {

    }
}
