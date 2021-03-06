/*******************************************************************************
 * Copyright (C) 2013  Stefan Schroeder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.constraint.SoftRouteConstraint;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;*/
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

final class JobInsertionConsideringFixCostsCalculator implements JobInsertionCostsCalculator, SoftRouteConstraint {

    private static final Logger logger = LoggerFactory.getLogger(JobInsertionConsideringFixCostsCalculator.class);

    private final JobInsertionCostsCalculator standardServiceInsertion;

    private double weight_deltaFixCost = 0.5;

    private double solution_completeness_ratio = 0.5;

    private RouteAndActivityStateGetter stateGetter;

    public JobInsertionConsideringFixCostsCalculator(final JobInsertionCostsCalculator standardInsertionCalculator, RouteAndActivityStateGetter stateGetter) {
        super();
        this.standardServiceInsertion = standardInsertionCalculator;
        this.stateGetter = stateGetter;
        logger.debug("inialise {}", this);
    }

    @Override
    public InsertionData getInsertionData(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle, double newVehicleDepartureTime, final Driver newDriver, final double bestKnownPrice) {
        double fixcost_contribution = getFixCostContribution(currentRoute, jobToInsert, newVehicle);
        if (fixcost_contribution > bestKnownPrice) {
            return InsertionData.createEmptyInsertionData();
        }
        InsertionData iData = standardServiceInsertion.getInsertionData(currentRoute, jobToInsert, newVehicle, newVehicleDepartureTime, newDriver, bestKnownPrice);
        if (iData instanceof InsertionData.NoInsertionFound) {
            return iData;
        }
        double totalInsertionCost = iData.getInsertionCost() + fixcost_contribution;
        InsertionData insertionData = new InsertionData(totalInsertionCost, iData.getPickupInsertionIndex(), iData.getDeliveryInsertionIndex(), newVehicle, newDriver);
        insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
        insertionData.getEvents().addAll(iData.getEvents());
        return insertionData;
    }

    private double getFixCostContribution(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle) {
        Capacity currentMaxLoadInRoute = getCurrentMaxLoadInRoute(currentRoute);
        double relFixCost = getDeltaRelativeFixCost(currentRoute, newVehicle, jobToInsert,currentMaxLoadInRoute);
        double absFixCost = getDeltaAbsoluteFixCost(currentRoute, newVehicle, jobToInsert,currentMaxLoadInRoute);
        double deltaFixCost = (1 - solution_completeness_ratio) * relFixCost + solution_completeness_ratio * absFixCost;
        double fixcost_contribution = weight_deltaFixCost * solution_completeness_ratio * deltaFixCost;
        return fixcost_contribution;
    }

    public void setWeightOfFixCost(double weight) {
        weight_deltaFixCost = weight;
        logger.debug("set weightOfFixCostSaving to {}", weight);
    }

    @Override
    public String toString() {
        return "[name=calculatesServiceInsertionConsideringFixCost][weightOfFixedCostSavings=" + weight_deltaFixCost + "]";
    }

    public void setSolutionCompletenessRatio(double ratio) {
        solution_completeness_ratio = ratio;
    }

    private double getDeltaAbsoluteFixCost(VehicleRoute route, Vehicle newVehicle, Job job, Capacity currentMaxLoadInRoute) {
        Capacity load = Capacity.addup(currentMaxLoadInRoute, job.getSize());
        double currentFix = 0.0;
        if (route.getVehicle() != null) {
            if (!(route.getVehicle() instanceof VehicleImpl.NoVehicle)) {
                currentFix += route.getVehicle().getType().getVehicleCostParams().fix;
            }
        }
        if (!newVehicle.getType().getCapacityDimensions().isGreaterOrEqual(load)) {
            return Double.MAX_VALUE;
        }
        return newVehicle.getType().getVehicleCostParams().fix - currentFix;
    }

    private double getDeltaRelativeFixCost(VehicleRoute route, Vehicle newVehicle, Job job, Capacity currentLoad) {
        Capacity load = Capacity.addup(currentLoad, job.getSize());
        double currentRelFix = 0.0;
        if (route.getVehicle() != null) {
            if (!(route.getVehicle() instanceof VehicleImpl.NoVehicle)) {
                currentRelFix += route.getVehicle().getType().getVehicleCostParams().fix * Capacity.divide(currentLoad, route.getVehicle().getType().getCapacityDimensions());
            }
        }
        if (!newVehicle.getType().getCapacityDimensions().isGreaterOrEqual(load)) {
            return Double.MAX_VALUE;
        }
        double relativeFixCost = newVehicle.getType().getVehicleCostParams().fix * (Capacity.divide(load, newVehicle.getType().getCapacityDimensions())) - currentRelFix;
        return relativeFixCost;
    }

    private Capacity getCurrentMaxLoadInRoute(VehicleRoute route) {
        Capacity maxLoad = stateGetter.getRouteState(route, InternalStates.MAXLOAD, Capacity.class);
        if (maxLoad == null) maxLoad = Capacity.Builder.newInstance().build();
        return maxLoad;
    }

    @Override
    public double getCosts(JobInsertionContext insertionContext) {
        return getFixCostContribution(insertionContext.getRoute(), insertionContext.getJob(), insertionContext.getNewVehicle());
    }

}
