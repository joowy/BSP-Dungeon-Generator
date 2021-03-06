package src.dungeonmap;

import src.dungeonmap.common.*;
import java.util.ArrayList;

/**
 * BSP algorithm; divide the map until MIN_SIZE;then fill the leaves with a
 * room; Converted from actionScript to java
 * https://gamedevelopment.tutsplus.com/tutorials/how-to-use-bsp-trees-to-generate-game-maps--gamedev-12268
 */

class Leaf
{
  final private int MIN_LEAF_SIZE = 8;
  private int x;
  private int y;
  private int w;
  private int h;
  private Rectangle room; // composition; leaf has a Rectangle

  public Leaf leftChild;
  public Leaf rightChild;

  Leaf(int x, int y, int width, int height)
  {
    this.x = x;
    this.y = y;
    this.w = width;
    this.h = height;
  }

  public boolean splitLeaf()
  {
    if (leftChild != null || rightChild != null)
    {
      return false;
    }
    boolean splitHorizontal = Math.random() > 0.5;
    if (w > h && w / h <= 1.25)
    {
      splitHorizontal = false;
    }
    else if (h > w && h / w >= 1.25)
    {
      splitHorizontal = true;
    }

    int max = (splitHorizontal ? h : w) - MIN_LEAF_SIZE;
    if (max <= MIN_LEAF_SIZE)
    {
      return false;
    }

    int split = Randomizer.generate(MIN_LEAF_SIZE, max);
    if (splitHorizontal)
    {
      leftChild = new Leaf(x, y, w, split);
      rightChild = new Leaf(x, y + split, w, h - split);
    }
    else
    {
      leftChild = new Leaf(x, y, split, h);
      rightChild = new Leaf(x + split, y, w - split, h);
    }
    return true;
  }

  public void createRooms(BSPTree bspTreeInstance)
  {
    if (leftChild != null || rightChild != null)
    {
      if (leftChild != null)
      {
        leftChild.createRooms(bspTreeInstance);
      }
      if (rightChild != null)
      {
        rightChild.createRooms(bspTreeInstance);
      }
      if (leftChild != null && rightChild != null)
      {
        bspTreeInstance.createPath(leftChild.getRoom(), rightChild.getRoom());
      }
    }
    else
    {
      int width = Randomizer.generate(BSPTree.getROOM_MIN_SIZE(),
              Math.min(BSPTree.getROOM_MAX_SIZE(), this.w - 1));
      int height = Randomizer.generate(BSPTree.getROOM_MIN_SIZE(),
              Math.min(BSPTree.getROOM_MAX_SIZE(), this.h - 1));
      int x = Randomizer.generate(this.x, this.x + (this.w - 1) - width);
      int y = Randomizer.generate(this.y, this.y + (this.h - 1) - height);

      room = new Rectangle(x, y, width, height);

      bspTreeInstance.createRoom(room);

    }
  }

  public Rectangle getRoom()
  {
    if (this.room != null)
    {
      return this.room;
    }
    else
    {
      Rectangle lRoom = null;
      Rectangle rRoom = null;
      if (this.leftChild != null)
      {
        lRoom = this.leftChild.getRoom();
      }
      if (this.rightChild != null)
      {
        rRoom = this.rightChild.getRoom();
      }
      if (this.leftChild == null && this.rightChild == null)
      {
        return null;
      }
      else if (rRoom != null)
      {
        return lRoom;
      }
      else if (lRoom != null)
      {
        return rRoom;
      }

      else if (Math.random() < 0.5)
      {
        return lRoom;
      }
      else
      {
        return rRoom;
      }

    }
  }

  public int getWidth()
  {
    return w;
  }

  public int getHeight()
  {
    return h;
  }
}

public class BSPTree
{

  private int[][] tiles; // initialize the level array
  // NOTE: tweaking finals below will change generation algorithm
  final private static int MAX_LEAF_SIZE = 25; // leaf Section size limit
  final private static int ROOM_MAX_SIZE = 24;
  final private static int ROOM_MIN_SIZE = 7;

  private int mapWidth;
  private int mapHeight;

  private ArrayList<Leaf> leafs;

  /**
   * implementation of BSP on 2D matrix representing a map.
   * https://en.wikipedia.org/wiki/Binary_space_partitioning
   * 
   * @author Timothy Hely
   *         https://gamedevelopment.tutsplus.com/tutorials/how-to-use-bsp-trees-to-generate-game-maps--gamedev-12268
   * 
   * @param mapWidth
   * @param mapHeight
   * @return Constructor; Call generateLeafs() to return 2D array
   */
  public BSPTree(int mW, int mH)
  {
    this.mapWidth = mW;
    this.mapHeight = mH;
  }

  /**
   * 
   * @return 2D array of 1s and 0s. 1 for wall; 0 for floor.
   */
  public int[][] generateLeafs()
  {
    if (mapWidth < 15 || mapHeight < 15)
    {
      throw new java.lang.Error("map width and map height must be larger than 15");
    }

    // tiles = new int[mapWidth + 1][mapHeight + 1]; // +1 for creating a border of
    // walls
    tiles = new int[mapWidth][mapHeight]; // +1 for creating a border of walls

    for (int i = 0; i < mapWidth; i++)

    {
      for (int j = 0; j < mapHeight; j++)
      {
        tiles[i][j] = MyConstants.WALL;
      }
    }

    leafs = new ArrayList<Leaf>();

    Leaf root = new Leaf(0, 0, mapWidth, mapHeight);
    leafs.add(root);

    boolean didSplit = true;

    while (didSplit)
    {
      didSplit = false;
      for (int i = 0; i < leafs.size(); i++)
      {
        Leaf lHelper = leafs.get(i);
        if (lHelper.leftChild == null && lHelper.rightChild == null)
        {
          if (lHelper.getWidth() > MAX_LEAF_SIZE || lHelper.getHeight() > MAX_LEAF_SIZE
                  || Math.random() > 0.8)
          {
            if (lHelper.splitLeaf())
            {
              leafs.add(lHelper.leftChild);
              leafs.add(lHelper.rightChild);
              didSplit = true;
            }
          }
        }
      }

    }
    root.createRooms(this); // create the room using the leaf splits
    return tiles; // return the level array 1 for wall 0 for floor
  }

  /**
   * Modifies the level array based on the room that is passed into this method
   * 
   * @param room
   */
  public void createRoom(Rectangle room)
  {
    for (int i = room.getX1() + 1; i < room.getX2(); i++) // `room.getX1() + 1` +1 so that all rooms are surrounded by walls
    {
      for (int j = room.getY1() + 1; j < room.getY2(); j++)
      {
        tiles[i][j] = MyConstants.FLOOR;
      }
    }
  }

  /**
   * connect two rooms
   * 
   * @param roomLeft
   * @param roomRight
   */
  public void createPath(Rectangle roomLeft, Rectangle roomRight)
  {
    int leftX = roomLeft.getCenter().getX();
    int leftY = roomLeft.getCenter().getY();
    int rightX = roomRight.getCenter().getX();
    int rightY = roomRight.getCenter().getY();

    if (Math.random() > 0.5)
    {
      createHorizontalPath(leftX, rightX, leftY);
      createVerticalPath(leftY, rightY, rightX);
    }
    else
    {
      createVerticalPath(leftY, rightY, leftX);
      createHorizontalPath(leftX, rightX, rightY);
    }

  }

  private void createHorizontalPath(int x1, int x2, int y)
  {
    for (int i = Math.min(x1, x2); i < Math.max(x1, x2) + 1; i++)
    {
      tiles[i][y] = 0;
      // tiles[i][y + 1] = 0;
      // tiles[i][y - 1] = 0;
    }
  }

  private void createVerticalPath(int y1, int y2, int x)
  {
    for (int i = Math.min(y1, y2); i < Math.max(y1, y2) + 1; i++)
    {
      tiles[x][i] = 0;
      // tiles[x + 1][i] = 0;
      // tiles[x - 1][i] = 0;

    }
  }

  public ArrayList<Leaf> getLeafs()
  {
    return this.leafs;
  }

  public static int getMAX_LEAF_SIZE()
  {
    return MAX_LEAF_SIZE;
  }

  public static int getROOM_MAX_SIZE()
  {
    return ROOM_MAX_SIZE;
  }

  public static int getROOM_MIN_SIZE()
  {
    return ROOM_MIN_SIZE;
  }

  public int getMapHeight()
  {
    return this.mapHeight;
  }

  public int getMapWidth()
  {
    return this.mapWidth;
  }

}