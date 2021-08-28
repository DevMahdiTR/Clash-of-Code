import java.util.*;

// The Last Crusade - Episode 3
class Player {
  public static void main(String args[]) {
    Crusade crusade = new Crusade();
    crusade.readInitInput();
    crusade.gameLoop();
  }
}

class Crusade {
  final int DEFAULT_COORDINATE = -1;
  final String TOP = "TOP";
  final String LEFT = "LEFT";
  final String RIGHT = "RIGHT";

  Scanner in = new Scanner(System.in);
  int rooms[][]; // [y][x]
  int width; // number of columns
  int height; // number of rows
  int xExit;

  void readInitInput() {
    width = in.nextInt();
    height = in.nextInt();
    if (in.hasNextLine())
      in.nextLine();

    rooms = new int[height][width];

    for (int y = 0; y < height; ++y) {
      String[] roomTypes = in.nextLine().split(" ");
      for (int x = 0; x < width; ++x) {
        rooms[y][x] = Integer.parseInt(roomTypes[x]);
      }
    }
    xExit = in.nextInt();
  }

  void gameLoop() {
    State prevIndy = null;
    Command prevCommand = null;
    Command prevCommand2 = null;
    Command prevCommand3 = null;

    while (true) {
      int x = in.nextInt();
      int y = in.nextInt();
      String from = in.next();
      Node indy = new Node(x, y, from);
      Node exit = new Node(xExit, height - 1);

      List<Rock> rocks = new ArrayList<>();
      List<Rock> rocksCopy = new ArrayList<>(); // original state of the rocks
      int nbRocks = in.nextInt(); // the number of rocks currently in the grid.
      for (int i = 0; i < nbRocks; i++) {
        int xRock = in.nextInt();
        int yRock = in.nextInt();
        String entryRock = in.next();
        Rock rock = new Rock(xRock, yRock, entryRock);
        if (prevIndy != null && rock.equalPosition(prevIndy))
          rock.follow = true;
        rocks.add(rock);
        rocksCopy.add(new Rock(rock));
      }

      Node end = search(indy, exit);
      List<Node> path = generatePath(end);

      Command command = computePathRotation(path);
      command = computeRockRotation(rocks, rocksCopy, path, command);

      if (command != null) {
        if (prevCommand != null && allEqual(command, prevCommand, prevCommand2, prevCommand3)) {
          System.out.println("WAIT"); // could be CHANGED
        } else {
          // check if command is valid
          boolean valid = true;

          for (Rock rockCopy : rocksCopy) {
            if (rockCopy.x == command.xRotation && rockCopy.y == command.yRotation) {
              valid = false;
              System.out.println("WAIT");
              break;
            }
          }

          if (valid)
            System.out.println(command.toString());
        }
      } else {
        System.out.println("WAIT");
      }

      prevIndy = new State(indy);
      prevCommand3 = prevCommand2;
      prevCommand2 = prevCommand;
      prevCommand = command;
    }
  }

  boolean allEqual(Command command, Command prevCommand, Command prevCommand2, Command prevCommand3) {
    return command.equals(prevCommand) && command.equals(prevCommand2) && command.equals(prevCommand3);
  }

  // compute rotation to open a path to the exit if necessary
  Command computePathRotation(List<Node> path) {
    for (Node current : path) {
      if (current.entry.rotation != Rotation.NONE) {
        Command command = new Command();
        command.rotationNode = current;
        command.clockWise = current.entry.rotation != Rotation.COUNTERCLOCKWISE;
        command.xRotation = current.x;
        command.yRotation = current.y;
        return command;
      }
    }
    return null;
  }

  // compute rotation to block a rock if necessary
  Command computeRockRotation(List<Rock> rocks, List<Rock> rocksCopy, List<Node> path, Command command) {
    if (command == null ||
        command.rotationNode.distance > 1 &&
            (command.rotationNode.entry.rotation == Rotation.CLOCKWISE ||
                command.rotationNode.entry.rotation == Rotation.COUNTERCLOCKWISE) ||
        command.rotationNode.distance > 2 &&
            command.rotationNode.entry.rotation == Rotation.DOUBLE_CLOCKWISE) {
      // for all cells on the path to the exit
      for (int i = 0; i < path.size(); ++i) {
        Node current = path.get(i);

        // update each rock's state for this turn
        for (int j = 0; j < rocks.size(); ++j) {
          Rock rock = rocks.get(j);

          if (!isValidPosition(rock)) {
            rock.ignore = true;
            continue;
          }

          String entry = nextEntry(type(rock), rock.entry.from);
          update(rock, entry);
          boolean directCollision = rock.equalPosition(current);

          // check other ROCK locations
          for (int k = 0; k < j; ++k) {
            Rock rock2 = rocks.get(k);

            // collision with another rock?
            if (rock.x == rock2.x && rock.y == rock2.y && !directCollision) {
              rock.ignore = true;
              rock2.ignore = true;
            }
          }

          boolean swapCollision = false;
          if (i < path.size() - 1) {
            State next = path.get(i + 1);
            Rock rockNext = new Rock(rock);
            if (isValidPosition(rockNext)) {
              String from = nextEntry(type(rockNext), rockNext.entry.from);
              update(rockNext, from);
              swapCollision = rock.equalPosition(next) && rockNext.equalPosition(current);
            }
          }

          if (!rock.ignore && !rock.follow) {
            // collision? then check if we can block the rock on the next turn
            if ((directCollision || swapCollision)) {
              Rock rockNext = new Rock(rocksCopy.get(j));
              entry = nextEntry(type(rockNext), rockNext.entry.from);
              update(rockNext, entry);
              if (type(rockNext) > 1) {
                if (command == null)
                  command = new Command();
                command.xRotation = rockNext.x;
                command.yRotation = rockNext.y;
                command.clockWise = true; // doesn't matter for episode 2, could take type into account
                return command;
              } else {
                // check if we can cause a collision between 2 ROCKS in 2 turns with a ROTATION
                Rock rock1Next2 = new Rock(rocksCopy.get(j));
                entry = nextEntry(type(rock1Next2), rock1Next2.entry.from);
                update(rock1Next2, entry);

                entry = nextEntry(type(rock1Next2), rock1Next2.entry.from);
                update(rock1Next2, entry);

                // check other ROCK locations
                for (int k = 0; k < rocks.size(); ++k) {
                  if (k != j) {
                    Rock rock2Next = new Rock(rocksCopy.get(k));
                    entry = nextEntry(type(rock2Next), rock2Next.entry.from);
                    update(rock2Next, entry);

                    for (State neighbor : neighbors(rock2Next)) {
                      if (rock1Next2.equalPosition(neighbor)) {
                        if (command == null)
                          command = new Command();
                        command.xRotation = rock2Next.x;
                        command.yRotation = rock2Next.y;
                        command.clockWise = neighbor.entry.parentRotation == Rotation.CLOCKWISE;
                        return command;
                      }
                    }
                  }
                }
              }

            }
          }
        }
      }
    }

    return command;
  }

  boolean isValidPosition(State state) {
    return state.x >= 0 && state.x < width && state.y >= 0 && state.y < height;
  }

  private void update(Rock rock, String entry) {
    switch (entry) {
      case LEFT:
        rock.x += 1;
        rock.entry.from = LEFT;
        break;
      case RIGHT:
        rock.x -= 1;
        rock.entry.from = RIGHT;
        break;
      case TOP:
        rock.y += 1;
        rock.entry.from = TOP;
    }
    rock.distance++;
  }

  // Compute a path between start and exit using DFS
  Node search(Node start, Node exit) {
    // initialize the grid
    Cell[][] cells = new Cell[height][width];
    for (int y = 0; y < height; ++y)
      for (int x = 0; x < width; ++x)
        cells[y][x] = new Cell(x, y);
    Stack<Node> stack = new Stack<>();
    Cell startCell = cells[start.y][start.x];
    startCell.visited.add(start.entry.from);
    start.distance = 0;
    stack.push(start);

    while (!stack.isEmpty()) {
      Node current = stack.pop();
      if (current.parent != null)
        current.parent.entry.rotation = current.entry.parentRotation;
      if (current.equalPosition(exit))
        return current;
      for (Node neighbor : neighbors(cells, current))
        stack.push(neighbor);
    }
    return null;
  }

  List<Node> generatePath(Node end) {
    List<Node> list = new ArrayList<>();
    Node current = end;
    while (current.parent != null) {
      list.add(0, current);
      current = current.parent;
    }
    return list;
  }

  String nextEntry(int type, String from) {
    switch (Math.abs(type)) {
      case 1:
        return TOP;
      case 2:
      case 6:
        if (from.equals(LEFT)) return LEFT;
        else if (from.equals(RIGHT)) return RIGHT;
        break;
      case 3:
        if (from.equals(TOP)) return TOP;
        break;
      case 4:
        if (from.equals(TOP)) return RIGHT;
        else if (from.equals(RIGHT)) return TOP;
        break;
      case 5:
        if (from.equals(TOP)) return LEFT;
        else if (from.equals(LEFT)) return TOP;
        break;
      case 7:
        if (from.equals(TOP) || from.equals(RIGHT)) return TOP;
        break;
      case 8:
        if (from.equals(LEFT) || from.equals(RIGHT)) return TOP;
        break;
      case 9:
        if (from.equals(TOP) || from.equals(LEFT)) return TOP;
        break;
      case 10:
        if (from.equals(TOP)) return RIGHT;
        break;
      case 11:
        if (from.equals(TOP)) return LEFT;
        break;
      case 12:
        if (from.equals(RIGHT)) return TOP;
        break;
      case 13:
        if (from.equals(LEFT)) return TOP;
        break;
    }
    return "";
  }

  List<Entry> nextEntries(State current) {
    List<Entry> entries = new ArrayList<>();
    int type = type(current);
    String prevEntry = current.entry.from;

    String from1 = nextEntry(type, prevEntry);
    entries.add(new Entry(from1));

    if (current.distance > 0) {
      if (type > 1) {
        String from2 = nextEntry(rotateClockwise(type), prevEntry);
        Entry entry2 = new Entry(from2);
        entry2.parentRotation = Rotation.CLOCKWISE;
        entries.add(entry2);
      }
      if (type > 5) {
        String from3 = nextEntry(rotateCounterclockwise(type), prevEntry);
        Entry entry3 = new Entry(from3);
        entry3.parentRotation = Rotation.COUNTERCLOCKWISE;
        entries.add(entry3);
      }
    }

    if (type > 5 && current.distance > 1) {
      String from4 = nextEntry(rotateClockwise(rotateClockwise(type)), prevEntry);
      Entry entry4 = new Entry(from4);
      entry4.parentRotation = Rotation.DOUBLE_CLOCKWISE;
      entries.add(entry4);
    }

    return entries;
  }

  List<Node> neighbors(Cell[][] cells, Node current) {
    List<Node> neighbors = new ArrayList<>();
    List<Entry> entries = nextEntries(current);
    for (Entry entry : entries) {
      String from = entry.from;
      if (from.equals(LEFT) && current.x < width - 1) {
        Node leftNeighbor = new Node(current.x + 1, current.y);
        update(cells, neighbors, current, leftNeighbor, entry);
      } else if (from.equals(RIGHT) && current.x > 0) {
        Node rightNeighbor = new Node(current.x - 1, current.y);
        update(cells, neighbors, current, rightNeighbor, entry);
      } else if (from.equals(TOP) && current.y < height - 1) {
        Node topNeighbor = new Node(current.x, current.y + 1);
        update(cells, neighbors, current, topNeighbor, entry);
      }
    }
    return neighbors;
  }

  // for ROCK interception
  void update(Cell[][] cells, List<Node> neighbors, Node current, Node neighbor, Entry entry) {
    Cell neighborCell = cells[neighbor.y][neighbor.x];
    if (type(neighbor) != 0 && !neighborCell.visited.contains(entry.from)) {
      neighbor.entry = entry;
      neighborCell.visited.add(entry.from);
      neighbor.distance = current.distance + 1;
      neighbor.parent = current;
      neighbors.add(neighbor);
    }
  }

  List<State> neighbors(State current) {
    List<State> neighbors = new ArrayList<>();
    List<Entry> entries = nextEntries(current);
    for (Entry entry : entries) {
      String from = entry.from;
      if (from.equals(LEFT) && current.x < width - 1) {
        State leftNeighbor = new State(current.x + 1, current.y);
        update(neighbors, current, leftNeighbor, entry);
      } else if (from.equals(RIGHT) && current.x > 0) {
        State rightNeighbor = new State(current.x - 1, current.y);
        update(neighbors, current, rightNeighbor, entry);
      } else if (from.equals(TOP) && current.y < height - 1) {
        State topNeighbor = new State(current.x, current.y + 1);
        update(neighbors, current, topNeighbor, entry);
      }
    }
    return neighbors;
  }

  void update(List<State> neighbors, State current, State neighbor, Entry entry) {
    if (type(neighbor) != 0) {
      neighbor.entry = entry;
      neighbor.distance = current.distance + 1;
      neighbors.add(neighbor);
    }
  }

  int rotateClockwise(int type) {
    switch (type) {
      case 2:
        return 3;
      case 3:
        return 2;
      case 4:
        return 5;
      case 5:
        return 4;
      case 6:
        return 7;
      case 7:
        return 8;
      case 8:
        return 9;
      case 9:
        return 6;
      case 10:
        return 11;
      case 11:
        return 12;
      case 12:
        return 13;
      case 13:
        return 10;
      default:
        return 0;
    }
  }

  int rotateCounterclockwise(int type) {
    switch (type) {
      case 6:
        return 9;
      case 7:
        return 6;
      case 8:
        return 7;
      case 9:
        return 8;
      case 10:
        return 13;
      case 11:
        return 10;
      case 12:
        return 11;
      case 13:
        return 12;
      default:
        return 0;
    }
  }

  int type(State state) {
    return rooms[state.y][state.x];
  }

  class State {
    int x, y;
    Entry entry = new Entry();
    int distance = 0;

    State() {}

    State(int x, int y) {
      this.x = x;
      this.y = y;
    }

    State(int x, int y, String from) {
      this(x, y);
      this.entry.from = from;
    }

    State(State other) {
      this(other.x, other.y, other.entry.from);
      distance = other.distance;
    }

    boolean equalPosition(State other) {
      return x == other.x && y == other.y;
    }
  }

  class Node extends State {
    Node parent = null;

    Node(int x, int y) {
      super(x, y);
    }

    Node(int x, int y, String from) {
      super(x, y, from);
    }
  }

  class Rock extends State {
    boolean ignore = false; // ignore during computation
    boolean follow = false; // rock which follows Indy behind him

    Rock(int x, int y, String from) {
      super(x, y, from);
    }

    Rock(Rock other) {
      x = other.x;
      y = other.y;
      entry.from = other.entry.from;
      entry.rotation = other.entry.rotation;
      ignore = other.ignore;
      follow = other.follow;
    }
  }

  class Cell {
    int x, y;
    Set<String> visited = new TreeSet<>(); // to check for repeat states

    Cell(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  class Entry {
    String from = "";
    Rotation rotation = Rotation.NONE;
    Rotation parentRotation = Rotation.NONE;

    Entry() {}

    Entry(String from) {
      this.from = from;
    }
  }

  enum Rotation {
    NONE,
    CLOCKWISE,
    COUNTERCLOCKWISE,
    DOUBLE_CLOCKWISE
  }

  class Command {
    boolean clockWise = false;
    int xRotation = DEFAULT_COORDINATE;
    int yRotation = DEFAULT_COORDINATE;
    Node rotationNode = null;

    boolean equals(Command other) {
      return other != null &&
          xRotation == other.xRotation && yRotation == other.yRotation && clockWise == other.clockWise;
    }

    @Override
    public String toString() {
      if (rotationNode != null) {
        StringBuilder output = new StringBuilder();
        output.append(xRotation);
        output.append(" ");
        output.append(yRotation);
        output.append(" ");
        if (clockWise) {
          output.append(RIGHT);
          rooms[yRotation][xRotation] = rotateClockwise(rooms[yRotation][xRotation]);
        } else {
          output.append(LEFT);
          rooms[yRotation][xRotation] = rotateCounterclockwise(rooms[yRotation][xRotation]);
        }
        return output.toString();
      }
      return "WAIT";
    }
  }
}
