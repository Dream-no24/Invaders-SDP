package entity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.Timer;

import Enemy.PiercingBullet;
import Enemy.HpEnemyShip;
import Sound_Operator.SoundManager;
import inventory_develop.Bomb;
import screen.Screen;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;
import Enemy.PiercingBulletPool;
/**
 * Groups enemy ships into a formation that moves together.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class EnemyShipFormation implements Iterable<EnemyShip> {
	// Sound Operator
	private static SoundManager sm;

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 20;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 100;
	/** Distance between ships. */
	private static final int SEPARATION_DISTANCE = 40;
	/** Proportion of C-type ships. */
	private static final double PROPORTION_C = 0.2;
	/** Proportion of B-type ships. */
	private static final double PROPORTION_B = 0.4;
	/** Lateral speed of the formation. */
	private static final int X_SPEED = 8;
	/** Downwards speed of the formation. */
	private static final int Y_SPEED = 4;
	/** Speed of the bullets shot by the members. */
	private static final int BULLET_SPEED = 4;
	/** Proportion of differences between shooting times. */
	private static final double SHOOTING_VARIANCE = .2;
	/** Margin on the sides of the screen. */
	private static final int SIDE_MARGIN = 20;
	/** Margin on the bottom of the screen. */
	private static final int BOTTOM_MARGIN = 80;
	/** Distance to go down each pass. */
	private static final int DESCENT_DISTANCE = 20;
	/** Minimum speed allowed. */
	private static final int MINIMUM_SPEED = 10;

	/** DrawManager instance. */
	private DrawManager drawManager;
	/** Application logger. */
	private Logger logger;
	/** Screen to draw ships on. */
	private Screen screen;

	/** List of enemy ships forming the formation. */
	private List<List<EnemyShip>> enemyShips;
	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Number of ships in the formation - horizontally. */
	private int nShipsWide;
	/** Number of ships in the formation - vertically. */
	private int nShipsHigh;
	/** Time between shots. */
	private int shootingInterval;
	/** Variance in the time between shots. */
	private int shootingVariance;
	/** Initial ship speed. */
	private int baseSpeed;
	/** Speed of the ships. */
	private int movementSpeed;
	/** Current direction the formation is moving on. */
	private Direction currentDirection;
	/** Direction the formation was moving previously. */
	private Direction previousDirection;
	/** Interval between movements, in frames. */
	private int movementInterval;
	/** Total width of the formation. */
	private int width;
	/** Total height of the formation. */
	private int height;
	/** Position in the x-axis of the upper left corner of the formation. */
	private int positionX;
	/** Position in the y-axis of the upper left corner of the formation. */
	private int positionY;
	/** Width of one ship. */
	private int shipWidth;
	/** Height of one ship. */
	private int shipHeight;
	/** List of ships that are able to shoot. */
	private List<EnemyShip> shooters;
	/** Number of not destroyed ships. */
	private int shipCount;

	/** Directions the formation can move. */
	private enum Direction {
		/** Movement to the right side of the screen. */
		RIGHT,
		/** Movement to the left side of the screen. */
		LEFT,
		/** Movement to the bottom of the screen. */
		DOWN
	};

	/**
	 * Constructor, sets the initial conditions.
	 * 
	 * @param gameSettings
	 *            Current game settings.
	 */
	public EnemyShipFormation(final GameSettings gameSettings) {
		this.drawManager = Core.getDrawManager();
		this.logger = Core.getLogger();
		this.enemyShips = new ArrayList<List<EnemyShip>>();
		this.currentDirection = Direction.RIGHT;
		this.movementInterval = 0;
		this.nShipsWide = gameSettings.getFormationWidth();
		this.nShipsHigh = gameSettings.getFormationHeight();
		this.shootingInterval = gameSettings.getShootingFrecuency();
		this.shootingVariance = (int) (gameSettings.getShootingFrecuency()
				* SHOOTING_VARIANCE);
		this.baseSpeed = gameSettings.getBaseSpeed();
		this.movementSpeed = this.baseSpeed;
		this.positionX = INIT_POS_X;
		this.positionY = INIT_POS_Y;
		this.shooters = new ArrayList<EnemyShip>();
		SpriteType spriteType;
		int hp=1;// Edited by Enemy

		this.logger.info("Initializing " + nShipsWide + "x" + nShipsHigh
				+ " ship formation in (" + positionX + "," + positionY + ")");

		// Each sub-list is a column on the formation.
		for (int i = 0; i < this.nShipsWide; i++)
			this.enemyShips.add(new ArrayList<EnemyShip>());

		for (List<EnemyShip> column : this.enemyShips) {
			for (int i = 0; i < this.nShipsHigh; i++) {
				if (shipCount == (nShipsHigh*1)+1 ||shipCount == (nShipsHigh*3)+1) //Edited by Enemy
					spriteType = SpriteType.ExplosiveEnemyShip1;
				else if (i / (float) this.nShipsHigh < PROPORTION_C)
					spriteType = SpriteType.EnemyShipC1;
				else if (i / (float) this.nShipsHigh < PROPORTION_B
						+ PROPORTION_C)
					spriteType = SpriteType.EnemyShipB1;
				else
					spriteType = SpriteType.EnemyShipA1;

				if(shipCount == nShipsHigh*(nShipsWide/2))
					hp = 2; // Edited by Enemy, It just example to insert EnmyShip that hp is 2.

				column.add(new EnemyShip((SEPARATION_DISTANCE
						* this.enemyShips.indexOf(column))
								+ positionX, (SEPARATION_DISTANCE * i)
								+ positionY, spriteType,hp));// Edited by Enemy
				this.shipCount++;
				hp = 1;// Edited by Enemy
			}
		}

		this.shipWidth = this.enemyShips.get(0).get(0).getWidth();
		this.shipHeight = this.enemyShips.get(0).get(0).getHeight();

		this.width = (this.nShipsWide - 1) * SEPARATION_DISTANCE
				+ this.shipWidth;
		this.height = (this.nShipsHigh - 1) * SEPARATION_DISTANCE
				+ this.shipHeight;

		for (List<EnemyShip> column : this.enemyShips)
			this.shooters.add(column.get(column.size() - 1));

	}

	/**
	 * Associates the formation to a given screen.
	 * 
	 * @param newScreen
	 *            Screen to attach.
	 */
	public final void attach(final Screen newScreen) {
		screen = newScreen;
	}

	/**
	 * Draws every individual component of the formation.
	 */
	public final void draw() {
		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				drawManager.drawEntity(enemyShip, enemyShip.getPositionX(),
						enemyShip.getPositionY());
	}

	/**
	 * Updates the position of the ships.
	 */
	public final void update() {
		if(this.shootingCooldown == null) {
			this.shootingCooldown = Core.getVariableCooldown(shootingInterval,
					shootingVariance);
			this.shootingCooldown.reset();
		}
		
		cleanUp();

		int movementX = 0;
		int movementY = 0;
		double remainingProportion = (double) this.shipCount
				/ (this.nShipsHigh * this.nShipsWide);
		this.movementSpeed = (int) (Math.pow(remainingProportion, 2)
				* this.baseSpeed);
		this.movementSpeed += MINIMUM_SPEED;
		
		movementInterval++;
		if (movementInterval >= this.movementSpeed) {
			movementInterval = 0;

			boolean isAtBottom = positionY
					+ this.height > screen.getHeight() - BOTTOM_MARGIN;
			boolean isAtRightSide = positionX
					+ this.width >= screen.getWidth() - SIDE_MARGIN;
			boolean isAtLeftSide = positionX <= SIDE_MARGIN;
			boolean isAtHorizontalAltitude = positionY % DESCENT_DISTANCE == 0;

			if (currentDirection == Direction.DOWN) {
				if (isAtHorizontalAltitude)
					if (previousDirection == Direction.RIGHT) {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 1");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 2");
					}
			} else if (currentDirection == Direction.LEFT) {
				if (isAtLeftSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 3");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 4");
					}
			} else {
				if (isAtRightSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 5");
					} else {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 6");
					}
			}

			if (currentDirection == Direction.RIGHT)
				movementX = X_SPEED;
			else if (currentDirection == Direction.LEFT)
				movementX = -X_SPEED;
			else
				movementY = Y_SPEED;

			positionX += movementX;
			positionY += movementY;

			// Cleans explosions.
			List<EnemyShip> destroyed;
			for (List<EnemyShip> column : this.enemyShips) {
				destroyed = new ArrayList<EnemyShip>();
				for (EnemyShip ship : column) {
					if (ship != null && ship.isDestroyed()) {
						destroyed.add(ship);
						this.logger.info("Removed enemy "
								+ column.indexOf(ship) + " from column "
								+ this.enemyShips.indexOf(column));
					}
				}
				column.removeAll(destroyed);
			}

			for (List<EnemyShip> column : this.enemyShips)
				for (EnemyShip enemyShip : column) {
					enemyShip.move(movementX, movementY);
					enemyShip.update();
				}
		}
	}

	/**
	 * Cleans empty columns, adjusts the width and height of the formation.
	 */
	private void cleanUp() {
		Set<Integer> emptyColumns = new HashSet<Integer>();
		int maxColumn = 0;
		int minPositionY = Integer.MAX_VALUE;
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				// Height of this column
				int columnSize = column.get(column.size() - 1).positionY
						- this.positionY + this.shipHeight;
				maxColumn = Math.max(maxColumn, columnSize);
				minPositionY = Math.min(minPositionY, column.get(0)
						.getPositionY());
			} else {
				// Empty column, we remove it.
				emptyColumns.add(this.enemyShips.indexOf(column));
			}
		}
		for (int index : emptyColumns) {
			this.enemyShips.remove(index);
			logger.info("Removed column " + index);
		}

		int leftMostPoint = 0;
		int rightMostPoint = 0;
		
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				if (leftMostPoint == 0)
					leftMostPoint = column.get(0).getPositionX();
				rightMostPoint = column.get(0).getPositionX();
			}
		}

		this.width = rightMostPoint - leftMostPoint + this.shipWidth;
		this.height = maxColumn;

		this.positionX = leftMostPoint;
		this.positionY = minPositionY;
	}

	/**
	 * Shoots a bullet downwards.
	 * 
	 * @param bullets
	 *            Bullets set to add the bullet being shot.
	 */
	public final void shoot(final Set<PiercingBullet> bullets) { // Edited by Enemy
		// For now, only ships in the bottom row are able to shoot.
		int index = (int) (Math.random() * this.shooters.size());
		EnemyShip shooter = this.shooters.get(index);
		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();
			sm = SoundManager.getInstance();
			sm.playES("Enemy_Gun_Shot_1_ES");
			bullets.add(PiercingBulletPool.getPiercingBullet( // Edited by Enemy
					shooter.getPositionX() + shooter.width / 2,
					shooter.getPositionY(),
					BULLET_SPEED,
					0)); // Edited by Enemy
		}
	}

	/**
	 * Destroys a ship.
	 *
	 * @param destroyedShip
	 *            Ship to be destroyed.
	 */
	public final void destroy(final EnemyShip destroyedShip) {
			if (Bomb.getIsBomb()) {		// team Inventory
				Bomb.destroyByBomb(enemyShips, destroyedShip, this.logger);
			} else {
				for (List<EnemyShip> column : this.enemyShips)
					for (int i = 0; i < column.size(); i++)
						if (column.get(i).equals(destroyedShip)) {
							column.get(i).destroy();
							this.logger.info("Destroyed ship in ("
									+ this.enemyShips.indexOf(column) + "," + i + ")");
						}
			}

		// Updates the list of ships that can shoot the player.
		if (this.shooters.contains(destroyedShip)) {
			int destroyedShipIndex = this.shooters.indexOf(destroyedShip);
			int destroyedShipColumnIndex = -1;

			for (List<EnemyShip> column : this.enemyShips)
				if (column.contains(destroyedShip)) {
					destroyedShipColumnIndex = this.enemyShips.indexOf(column);
					break;
				}

			EnemyShip nextShooter = getNextShooter(this.enemyShips
					.get(destroyedShipColumnIndex));

			if (nextShooter != null)
				this.shooters.set(destroyedShipIndex, nextShooter);
			else {
				this.shooters.remove(destroyedShipIndex);
				this.logger.info("Shooters list reduced to "
						+ this.shooters.size() + " members.");
			}
		}

		this.shipCount--;
	}
	/**
	 * Gets the ship on a given column that will be in charge of shooting.
	 * 
	 * @param column
	 *            Column to search.
	 * @return New shooter ship.
	 */
	public final EnemyShip getNextShooter(final List<EnemyShip> column) {
		Iterator<EnemyShip> iterator = column.iterator();
		EnemyShip nextShooter = null;
		while (iterator.hasNext()) {
			EnemyShip checkShip = iterator.next();
			if (checkShip != null && !checkShip.isDestroyed())
				nextShooter = checkShip;
		}

		return nextShooter;
	}

	/**
	 * Returns an iterator over the ships in the formation.
	 * 
	 * @return Iterator over the enemy ships.
	 */
	@Override
	public final Iterator<EnemyShip> iterator() {
		Set<EnemyShip> enemyShipsList = new HashSet<EnemyShip>();

		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				enemyShipsList.add(enemyShip);

		return enemyShipsList.iterator();
	}

	/**
	 * Checks if there are any ships remaining.
	 * 
	 * @return True when all ships have been destroyed.
	 */
	public final boolean isEmpty() {
		return this.shipCount <= 0;
	}

	/**
	 * When EnemyShip is hit, its HP decrease by 1, and if the HP reaches 0, the ship is destroyed.
	 *
	 * @param destroyedShip
	 *            Ship to be hit
	 */
	public final int[] _destroy(final Bullet bullet, final EnemyShip destroyedShip) {// Edited by Enemy team
		int count = 0;	// number of destroyed enemy
		int point = 0;  // point of destroyed enemy

		if (bullet.getSpriteType() == SpriteType.ItemBomb) { // team Inventory
			int[] temp = Bomb.destroyByBomb(enemyShips, destroyedShip, this.logger);
			count = temp[0];
			point = temp[1];
		} else {
			for (List<EnemyShip> column : this.enemyShips) // Add by Enemy team
				for (int i = 0; i < column.size(); i++) {
					if (column.get(i).equals(destroyedShip)) {
						switch (destroyedShip.spriteType){
							case ExplosiveEnemyShip1:
							case ExplosiveEnemyShip2:
								HpEnemyShip.hit(destroyedShip);
								explosive(this.enemyShips.indexOf(column),i);
								count += findEnemy(this.enemyShips.indexOf(column),i)+1;
								this.logger.info(count+"@@@@@@@@@@@@@@@@@@@@@");
//								int a = 0;
//								a = findEnemy(this.enemyShips.indexOf(column),i);
//								this.logger.info(a+"---------------------------");
//								HpEnemyShip.hit(destroyedShip);
//								for (List<EnemyShip> enemyShip : this.enemyShips)
//									if (enemyShip.size() > i
//											&& !enemyShip.get(i).isDestroyed())
//										this._destroy(bullet, enemyShip.get(i));
//								for (int j = 0; j < column.size(); j++)
//									if (!column.get(j).isDestroyed())
//										this._destroy(bullet, column.get(j));
								break;
							default:
									HpEnemyShip.hit(destroyedShip);
									if(destroyedShip.isDestroyed())
										count ++;
								break;
						}
//						if (column.get(i).getHp() > 0) {
//							this.logger.info("Enemy ship lost 1 HP in ("
//									+ this.enemyShips.indexOf(column) + "," + i + ")");
//						}
//						else{
//							this.logger.info("Destroyed ship in ("
//									+ this.enemyShips.indexOf(column) + "," + i + ")");
//
//							point = column.get(i).getPointValue();
//							count += 1;
//						}
					}
				}
		}

		// Updates the list of ships that can shoot the player.
		if (bullet.getSpriteType() == SpriteType.ItemBomb) {	// team Inventory
			Bomb.nextShooterByBomb(enemyShips, shooters, this, logger);

		} else if (destroyedShip.isDestroyed()) {
			if (this.shooters.contains(destroyedShip)) {
				int destroyedShipIndex = this.shooters.indexOf(destroyedShip);
				int destroyedShipColumnIndex = -1;

				for (List<EnemyShip> column : this.enemyShips)
					if (column.contains(destroyedShip)) {
						destroyedShipColumnIndex = this.enemyShips.indexOf(column);
						break;
					}

				EnemyShip nextShooter = getNextShooter(this.enemyShips
						.get(destroyedShipColumnIndex));

				if (nextShooter != null)
					this.shooters.set(destroyedShipIndex, nextShooter);
				else {
					this.shooters.remove(destroyedShipIndex);
					this.logger.info("Shooters list reduced to "
							+ this.shooters.size() + " members.");
				}

			}
		}
		this.shipCount -= count;
		this.logger.info(this.shipCount+"-------------");
		int[] returnValue = {count, point};
		return returnValue;
	}

	public final int findEnemy(final int x, final int y) {
		int count = 0;

		// 1. column check
		for (int i = 0; i < this.enemyShips.size(); i++) {
			if (y < this.enemyShips.get(i).size()) {
				if (!this.enemyShips.get(i).get(y).isDestroyed() && this.enemyShips.get(i).get(y).getHp() == 1) {
					count++;
				}
			}
		}

		// 2. row check
		if (x < this.enemyShips.size()) {
			for (int i = 0; i < this.enemyShips.get(x).size(); i++) {
				if (!this.enemyShips.get(x).get(i).isDestroyed() && this.enemyShips.get(x).get(i).getHp() == 1) {
					count++;
				}
			}
		}

		return count;
	}

	public void explosive(final int x, final int y) {
		javax.swing.Timer timer = new javax.swing.Timer(200, null);
		int max = Math.max(this.enemyShips.size(), this.enemyShips.get(x).size());
		final int[] count = {0};

		ActionListener listener = new ActionListener() {
			private int i = 1;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (y + i < enemyShips.get(x).size() &&
						!enemyShips.get(x).get(y + i).isDestroyed()) {
					HpEnemyShip.hit(enemyShips.get(x).get(y + i)); // down
				}

				if (y - i >= 0 &&
						!enemyShips.get(x).get(y - i).isDestroyed()) {
					HpEnemyShip.hit(enemyShips.get(x).get(y - i)); // up
				}

				if (x + i < enemyShips.size() && y < enemyShips.get(x + i).size() &&
						!enemyShips.get(x + i).get(y).isDestroyed()) {
					HpEnemyShip.hit(enemyShips.get(x + i).get(y)); // right
				}

				if (x - i >= 0 && y < enemyShips.get(x - i).size() &&
						!enemyShips.get(x - i).get(y).isDestroyed()) {
					HpEnemyShip.hit(enemyShips.get(x - i).get(y)); // left
				}

				i++;

				if (i >= max) {
					((Timer)e.getSource()).stop();
				}
			}
		};

		timer.addActionListener(listener);
		timer.start();
	}
}