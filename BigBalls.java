package pkg;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.Random;

public class BigBalls extends AdvancedRobot {
    Random random = new Random();
    final int safeDistanceToWall = 70;
    final double maxEscapeAngle = Math.toRadians(45);
	double previousEnemyEnergy = 100;
    long timeSinceLastMove = 0;
	final long maxStationaryTime = 20;
    public void run() {
        setColors(Color.blue, Color.black, Color.red);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        do {
            if(getTime() - timeSinceLastMove > maxStationaryTime){
				oscillatoryOrbitMovement();
			}
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }

    private void oscillatoryOrbitMovement() {
        double direction = Math.random() > 0.5 ? 1 : -1;
        double angle = 90 * direction;

        double futureX = getX() + Math.sin(Math.toRadians(getHeading() + angle)) * safeDistanceToWall;
        double futureY = getY() + Math.cos(Math.toRadians(getHeading() + angle)) * safeDistanceToWall;

        if (!isSafePosition(futureX, futureY)) {
            direction *= -1;
            angle = 120 * direction;
        }

        setAhead(100 * direction);
        setTurnRight(angle);
        execute();
    }

    private boolean isSafePosition(double x, double y) {
        return x > safeDistanceToWall && x < getBattleFieldWidth() - safeDistanceToWall
                && y > safeDistanceToWall && y < getBattleFieldHeight() - safeDistanceToWall;
    }
	
   public void onScannedRobot(ScannedRobotEvent e) {
        double changeInEnergy = previousEnemyEnergy - e.getEnergy();
        if (changeInEnergy >= 0.1 && changeInEnergy <= 3.0) {
            oscillatoryOrbitMovement();
        }
        previousEnemyEnergy = e.getEnergy();
        double radarTurn = Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians());
        setTurnRadarRightRadians(2.0 * radarTurn);

        double bulletPower = calculateBulletPower(e.getDistance());
        double bulletSpeed = 20 - 3 * bulletPower;
        double futureTime = e.getDistance() / bulletSpeed;
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double futureX = getX() + Math.sin(absoluteBearing) * e.getDistance() + 
                         Math.sin(e.getHeadingRadians()) * e.getVelocity() * futureTime;
        double futureY = getY() + Math.cos(absoluteBearing) * e.getDistance() + 
                         Math.cos(e.getHeadingRadians()) * e.getVelocity() * futureTime;
        double futureBearing = absoluteBearing(getX(), getY(), futureX, futureY);

        double gunTurnAmt = Utils.normalRelativeAngle(futureBearing - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurnAmt);
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
            fire(bulletPower);
        }
    }
	
    private double calculateBulletPower(double enemyDistance) {
        double power;
        if(getEnergy() > 50) {
            power = Math.min(3.0, 400 / enemyDistance);
        } else if(getEnergy() > 20 && getEnergy() <= 50) {
            power = Math.min(2.5, 300 / enemyDistance);
        } else {
            power = 1.0;
        }
        return power;
    }

    public double absoluteBearing(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.atan2(deltaX, deltaY);
    }
    
    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        oscillatoryOrbitMovement();
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        setBack(150);
        setTurnRight(90 + (random.nextDouble() * 180));
    }
}
