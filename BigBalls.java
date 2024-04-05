package pkg;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.Random;

public class BigBalls extends AdvancedRobot {
    Random random = new Random();
    final int safeDistanceToWall = 70; // Define uma distância segura das paredes
    final double maxEscapeAngle = Math.toRadians(45); // Ângulo máximo de escape
	double previousEnemyEnergy = 100; // To track enemy's energy changes
    long timeSinceLastMove = 0; // To ensure movement if stationary for too long
	final long maxStationaryTime = 20; // Max time to stay stationary (2 seconds)
    public void run() {
        setColors(Color.blue, Color.black, Color.red); // Corpo, Canhão, Radar
        setAdjustGunForRobotTurn(true); // Independência do canhão
        setAdjustRadarForGunTurn(true); // Independência do radar

        do {
            // Movimento oscilatório e de orbitação
            if(getTime() - timeSinceLastMove > maxStationaryTime){
				oscillatoryOrbitMovement();
			}
            // Varredura contínua do radar
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }

    // Implementação do movimento oscilatório e de orbitação com evasão de paredes
    private void oscillatoryOrbitMovement() {
        double direction = Math.random() > 0.5 ? 1 : -1; // Escolhe aleatoriamente para frente ou para trás
        double angle = 90 * direction; // Define um ângulo base para a virada

        // Calcula os movimentos para evitar bater nas paredes
        double futureX = getX() + Math.sin(Math.toRadians(getHeading() + angle)) * safeDistanceToWall;
        double futureY = getY() + Math.cos(Math.toRadians(getHeading() + angle)) * safeDistanceToWall;

        // Verifica se o movimento futuro resulta em colisão com a parede
        if (!isSafePosition(futureX, futureY)) {
            // Se não for seguro, inverte a direção e ajusta o ângulo
            direction *= -1;
            angle = 120 * direction; // Ajusta o ângulo para uma maior variação
        }

        // Executa o movimento e a rotação
        setAhead(100 * direction);
        setTurnRight(angle);
        execute();
    }

    private boolean isSafePosition(double x, double y) {
        // Verifica se a posição futura está dentro dos limites seguros do campo de batalha
        return x > safeDistanceToWall && x < getBattleFieldWidth() - safeDistanceToWall
                && y > safeDistanceToWall && y < getBattleFieldHeight() - safeDistanceToWall;
    }
	/*
	public void onScannedRobot(ScannedRobotEvent e) {
        // Adjust movement based on enemy's actions
        double changeInEnergy = previousEnemyEnergy - e.getEnergy();
        if (changeInEnergy >= 0.1 && changeInEnergy <= 3.0) {
            // Enemy has fired
            oscillatoryOrbitMovement();
        }
        previousEnemyEnergy = e.getEnergy();
        
        // Basic firing strategy
        double gunTurn = Utils.normalRelativeAngleDegrees(e.getBearing() + getHeading() - getGunHeading());
        setTurnGunRight(gunTurn);
        if (Math.abs(gunTurn) <= 10) { // Ensure the gun is aligned before firing
            fire(Math.min(400 / e.getDistance(), 3)); // Fire with power based on distance
        }
    }
	*/
   public void onScannedRobot(ScannedRobotEvent e) {
        double changeInEnergy = previousEnemyEnergy - e.getEnergy();
        if (changeInEnergy >= 0.1 && changeInEnergy <= 3.0) {
            // Enemy has fired
            oscillatoryOrbitMovement();
        }
        previousEnemyEnergy = e.getEnergy();
		// Gestão avançada do radar
        double radarTurn = Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians());
        setTurnRadarRightRadians(2.0 * radarTurn);

        // Estratégia de combate aprimorada
        double bulletPower = calculateBulletPower(e.getDistance());
        double bulletSpeed = 20 - 3 * bulletPower;
        double futureTime = e.getDistance() / bulletSpeed;
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double futureX = getX() + Math.sin(absoluteBearing) * e.getDistance() + 
                         Math.sin(e.getHeadingRadians()) * e.getVelocity() * futureTime;
        double futureY = getY() + Math.cos(absoluteBearing) * e.getDistance() + 
                         Math.cos(e.getHeadingRadians()) * e.getVelocity() * futureTime;
        double futureBearing = absoluteBearing(getX(), getY(), futureX, futureY);

        // Mirando e disparando no futuro previsto do inimigo
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
        // Movimento evasivo imediato ao ser atingido
        oscillatoryOrbitMovement();
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        // Reação aprimorada ao colidir com a parede
        setBack(150);
        setTurnRight(90 + (random.nextDouble() * 180));
    }
}
