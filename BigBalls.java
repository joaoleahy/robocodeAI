package pkg;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.Random;

public class BigBalls extends AdvancedRobot {
    Random random = new Random();
    final int safeDistanceToWall = 70;
    final double maxEscapeAngle = Math.toRadians(45);

    public void run() {
        setColors(Color.blue, Color.black, Color.red);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        do {
            oscillatoryOrbitMovement();
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }

    private void oscillatoryOrbitMovement() {
        setAhead(150 * (random.nextBoolean() ? 1 : -1));
        setTurnRight(90 * (random.nextBoolean() ? 1 : -1));
        execute();
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double radarTurn = 
            Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians());
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

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        oscillatoryOrbitMovement();
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        setBack(150);
        setTurnRight(90 + (random.nextDouble() * 180));
    }

    private boolean tooCloseToWall() {
        return (getX() <= safeDistanceToWall || getX() >= getBattleFieldWidth() - safeDistanceToWall ||
                getY() <= safeDistanceToWall || getY() >= getBattleFieldHeight() - safeDistanceToWall);
    }

    public double absoluteBearing(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.atan2(deltaX, deltaY);
    }
}
