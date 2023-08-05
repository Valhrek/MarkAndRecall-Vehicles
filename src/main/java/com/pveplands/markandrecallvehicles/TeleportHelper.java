package com.pveplands.markandrecallvehicles;
// Taken directly from bdew's highway portals

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.behaviours.Seat;
import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.behaviours.Vehicles;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class TeleportHelper {
	private static final Logger logger = Logger.getLogger("TeleportHelper");

	private static Stream<Player> getPlayerSafe(long id) {
        try {
            return Stream.of(Players.getInstance().getPlayer(id));
        } catch (NoSuchPlayerException e) {
            return Stream.empty();
        }
    }

    //public static void doTeleport(Creature performer, Item target) {
    public static void doTeleport(Creature performer, short tx2, short ty2, int layer, int floorLevel) {
    	try {
    		//logger.info("in doTeleport");
            Vehicle vehicle = null;
            long vehicleId = performer.getVehicle();
            if (vehicleId != -10) {
                vehicle = Vehicles.getVehicleForId(vehicleId);
                if (vehicle != null) {
                    String vehicleName = Vehicle.getVehicleName(vehicle);
                    if (vehicle.pilotId != performer.getWurmId()) {
                        performer.getCommunicator().sendNormalServerMessage(String.format("You disembark the %s and then step into the portal.", vehicleName));
                        Server.getInstance().broadCastAction(String.format("%s disembarks the %s and steps into a portal.", performer.getName(), vehicleName), performer, 5);
                        vehicle = null;
                    } else {
                        performer.getCommunicator().sendNormalServerMessage(String.format("You direct the %s into the portal.", vehicleName));
                        Server.getInstance().broadCastAction(String.format("%s directs %s into a portal.", performer.getName(), vehicleName), performer, 5);
                        Arrays.stream(vehicle.seats)
                                .filter(Seat::isOccupied)
                                .map(Seat::getOccupant)
                                .filter(v -> v != performer.getWurmId())
                                .flatMap(TeleportHelper::getPlayerSafe)
                                .forEach(p -> {
                                    performer.getCommunicator().sendNormalServerMessage(String.format("The %s disappears as it goes into a portal.", vehicleName));
                                    //for some reason this was originally false. Seems like it should be true
                                    //p.disembark(false);
                                    p.disembark(true);
                                });
                    }
                }
            } else {
                performer.getCommunicator().sendNormalServerMessage("You step into the portal.");
                Server.getInstance().broadCastAction(String.format("%s steps into a portal.", performer.getName()), performer, 5);
            }

            Map<Creature, Item> leadingItems = new HashMap<>();
            
            //logger.info(String.format("Initial Creatures being led: %d",performer.getNumberOfFollowers()));
            
            Arrays.stream(performer.getFollowers()).forEach(follower -> {
                leadingItems.put(follower, performer.getLeadingItem(follower));
                //stop leading
                performer.removeFollower(follower);
                follower.leader = null;
                
            });
            //logger.info(String.format("After remove Creatures being led: %d",performer.getNumberOfFollowers()));
            
            Item dragged = performer.getDraggedItem();

            //teleport the performer first
            //logger.info("calling teleportPerformer");
            
            //logger.info(String.format("Before performer teleport Creatures being led: %d",performer.getNumberOfFollowers()));
            
            teleportPerformer(performer, tx2, ty2, layer, floorLevel);
            //logger.info("after teleportPerformer");
            
            if (dragged != null) {
                teleportItem(dragged, performer);
                Items.startDragging(performer, dragged);
            }

            //logger.info(String.format("After teleport Creatures being led: %d",performer.getNumberOfFollowers()));
            leadingItems.forEach((follower, item) -> {
            	teleportCreature(follower, performer);
                follower.setLeader(performer);
                performer.addFollower(follower, item);
            });
            //logger.info(String.format("After creatures teleport Creatures being led: %d",performer.getNumberOfFollowers()));
            
            if (vehicle != null) {
                if (vehicle.isCreature()) {
                    teleportCreature(Creatures.getInstance().getCreature(vehicleId), performer);
                } else {
                    teleportItem(Items.getItem(vehicleId), performer);
                }
            }
            logger.info(String.format("After vehicle teleport Creatures being led: %d",performer.getNumberOfFollowers()));
            
        } catch (Exception e) {
            //HwPortals.logException(String.format("Error while teleporting %s to %d", performer.getName(), target.getWurmId()), e);
            performer.getCommunicator().sendAlertServerMessage("Something went wrong while teleporting, try again later or contact staff.");
        }
    }

  
    private static void teleportPerformer(Creature subject, short tx2, short ty2, int layer, int floorLevel) {
    	//logger.info("in teleportPerformer");
        
    	//float tx = target.getPosX() + Server.rand.nextFloat() - 0.5f;
    	//float tx = tx2 + Server.rand.nextFloat() - 0.5f;
    	//float ty = target.getPosY() + Server.rand.nextFloat() - 0.5f;
    	//float ty = ty2 + Server.rand.nextFloat() - 0.5f;
        //float tz = target.getPosZ() + Server.rand.nextFloat() - 0.5f;
    	//logger.log(Level.INFO, String.format("In teleportPerformer modified coords x: %f y: %f", tx, ty ));
    	
    	//using the teleport method that takes in short coords, float coords seem to fail
        subject.setTeleportPoints(tx2, ty2, layer, floorLevel);
        if (subject.startTeleporting()) {
            subject.getCommunicator().sendTeleport(false);
        }
    }

    private static void teleportCreature(Creature subject, Creature target) {
        float tx = target.getPosX() + Server.rand.nextFloat() - 0.5f;
        float ty = target.getPosY() + Server.rand.nextFloat() - 0.5f;
        float tz = target.getPositionZ() + Server.rand.nextFloat() - 0.5f;
        if (subject.isPlayer()) {
            subject.setTeleportPoints(tx, ty, target.isOnSurface() ? 0 : -1, 0);
            if (subject.startTeleporting()) {
                subject.getCommunicator().sendTeleport(false);
            }
        } else {
            CreatureBehaviour.blinkTo(subject, tx, ty, target.isOnSurface() ? 0 : -1, tz, -10L, 0);
        }
    }

    private static void teleportItem(Item subject, Creature target) throws NoSuchZoneException {
        Zone originZone = Zones.getZone(subject.getTilePos(), subject.isOnSurface());
        Zone targetZone = Zones.getZone(target.getTilePos(), target.isOnSurface());
        originZone.removeItem(subject);
        float tx = target.getPosX() + Server.rand.nextFloat() - 0.5f;
        float ty = target.getPosY() + Server.rand.nextFloat() - 0.5f;
        float tz = target.getPositionZ() + Server.rand.nextFloat() - 0.5f;
        //may need to adjust rotation??
        //subject.setPosXYZRotation(tx, ty, tz, target.);
        subject.setPosXYZ(tx, ty, tz);
        
        targetZone.addItem(subject);
    }
}
