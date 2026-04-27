import java.util.*;

public class Main {
    // Списки для хранения координат ловушек и аптечек
    private static List<Point> traps = new ArrayList<>();
    private static List<Point> medkits = new ArrayList<>();
    private static final int MAX_LIVES = 3;

    public static void main(String[] args) {
        String castle = "\uD83C\uDFF0";
        int sizeBoard = 5;

        Person person = new Person(sizeBoard);
        int step = 0;

        // Игровое поле (хранит статические объекты: замок, монстров)
        String[][] board = new String[sizeBoard][sizeBoard];
        for (int y = 0; y < sizeBoard; y++) {
            for (int x = 0; x < sizeBoard; x++) {
                board[y][x] = "  ";
            }
        }

        // Генерация монстров (как в оригинале)
        int countMonster = sizeBoard * sizeBoard - sizeBoard - 5;
        Random r = new Random();

        Monster[] arrMonster = new Monster[countMonster + 1];
        int count = 0;
        Monster test;
        while (count <= countMonster) {
            if (r.nextBoolean()) {
                test = new Monster(sizeBoard);
            } else {
                test = new BigMonster(sizeBoard);
            }
            if (board[test.getY()][test.getX()].equals("  ")) {
                board[test.getY()][test.getX()] = test.getImage();
                arrMonster[count] = test;
                count++;
            }
        }

        // Генерация замка (всегда в верхней строке, y=0)
        int castleX = r.nextInt(sizeBoard);
        int castleY = 0;
        board[castleY][castleX] = castle;

        // ---------- ГЕНЕРАЦИЯ ЛОВУШЕК И АПТЕЧЕК ----------
        generateTrapsAndMedkits(sizeBoard, 3, 1, board);

        // Начало игры
        System.out.println("Привет! Ты готов начать играть в игру? (Напиши: ДА или НЕТ)");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine();
        System.out.println("Ваш ответ:\t" + answer);

        switch (answer) {
            case "ДА" -> {
                System.out.println("Выбери сложность игры(от 1 до 5):");
                int difficultGame = sc.nextInt();
                System.out.println("Выбранная сложность:\t" + difficultGame);
                while (true) {
                    // Показываем поле с текущим положением героя
                    board[person.getY() - 1][person.getX() - 1] = person.getImage();
                    outputBoard(board, person.getLive(), sizeBoard);
                    board[person.getY() - 1][person.getX() - 1] = "  "; // убираем героя для логики хода

                    System.out.println("Введите куда будет ходить персонаж (ход возможен только по вертикали и горизонтали на одну клетку;" +
                            "\nКоординаты персонажа - (x: " + person.getX() + ", y: " + person.getY() + "))");
                    int x = sc.nextInt();
                    int y = sc.nextInt();

                    if (person.moveCorrect(x, y)) {
                        String next = board[y - 1][x - 1];
                        if (next.equals("  ")) {
                            // Проверка на ловушку или аптечку
                            if (isTrapAt(x - 1, y - 1)) {
                                person.downLive();
                                System.out.println("Вы наступили на ловушку! -1 жизнь. Осталось: " + person.getLive());
                                removeTrap(x - 1, y - 1);
                            } else if (isMedkitAt(x - 1, y - 1)) {
                                if (person.getLive() < MAX_LIVES) {
                                    person.addLive();
                                    System.out.println("Вы нашли аптечку! +1 жизнь. Теперь: " + person.getLive());
                                } else {
                                    System.out.println("У вас уже максимальное количество жизней (3). Аптечка не пригодилась.");
                                }
                                removeMedkit(x - 1, y - 1);
                            }
                            // Перемещение героя
                            board[person.getY() - 1][person.getX() - 1] = "  ";
                            person.move(x, y);
                            step++;
                            System.out.println("Ход корректный; Новые координаты: " + person.getX() + ", " + person.getY() +
                                    "\nХод номер: " + step);
                        } else if (next.equals(castle)) {
                            System.out.println("Вы прошли игру!");
                            break;
                        } else {
                            // Встретили монстра
                            for (Monster monster : arrMonster) {
                                if (monster.conflictPerson(x, y)) {
                                    if (monster.taskMonster(difficultGame)) {
                                        board[person.getY() - 1][person.getX() - 1] = "  ";
                                        person.move(x, y);
                                        // Здесь можно удалить монстра из массива (опционально)
                                    } else {
                                        person.downLive();
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        System.out.println("Некорректный ход");
                    }

                    // Проверка на окончание жизни
                    if (person.getLive() <= 0) {
                        System.out.println("GAME OVER! У вас закончились жизни.");
                        break;
                    }
                }
            }
            case "НЕТ" -> System.out.println("Жаль, приходи еще!");
            default -> System.out.println("Данные введены некорректно");
        }
        sc.close();
    }

    // ---------- МЕТОДЫ ДЛЯ ЛОВУШЕК И АПТЕЧЕК ----------
    static void generateTrapsAndMedkits(int size, int trapCount, int medkitCount, String[][] board) {
        // Собираем все пустые клетки (нет монстров, нет замка, нет героя – героя пока нет на поле)
        List<Point> emptyCells = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (board[y][x].equals("  ")) {
                    emptyCells.add(new Point(x, y));
                }
            }
        }
        Collections.shuffle(emptyCells);

        // Размещаем ловушки
        for (int i = 0; i < trapCount && i < emptyCells.size(); i++) {
            traps.add(emptyCells.get(i));
        }
        // Убираем ловушки из списка пустых клеток (создаём новый список для аптечек)
        List<Point> remaining = new ArrayList<>();
        for (int i = trapCount; i < emptyCells.size(); i++) {
            remaining.add(emptyCells.get(i));
        }
        // Размещаем аптечки
        for (int i = 0; i < medkitCount && i < remaining.size(); i++) {
            medkits.add(remaining.get(i));
        }
    }

    static boolean isTrapAt(int x, int y) {
        for (Point p : traps) {
            if (p.x == x && p.y == y) return true;
        }
        return false;
    }

    static boolean isMedkitAt(int x, int y) {
        for (Point p : medkits) {
            if (p.x == x && p.y == y) return true;
        }
        return false;
    }

    static void removeTrap(int x, int y) {
        traps.removeIf(p -> p.x == x && p.y == y);
    }

    static void removeMedkit(int x, int y) {
        medkits.removeIf(p -> p.x == x && p.y == y);
    }

    // ---------- ВЫВОД ИГРОВОГО ПОЛЯ ----------
    static void outputBoard(String[][] board, int live, int size) {
        String leftBlock = "| ";
        String rightBlock = "|";
        String wall = "+ —— + —— + —— + —— + —— +";

        for (int y = 0; y < size; y++) {
            System.out.println(wall);
            for (int x = 0; x < size; x++) {
                String cell = board[y][x];
                if (!cell.equals("  ")) {
                    // Замок, монстр или временно герой
                    System.out.print(leftBlock + cell + " ");
                } else {
                    // Пустая клетка – рисуем ловушку или аптечку
                    if (isTrapAt(x, y)) {
                        System.out.print(leftBlock + "☠" + " ");
                    } else if (isMedkitAt(x, y)) {
                        System.out.print(leftBlock + "❤" + " ");
                    } else {
                        System.out.print(leftBlock + "  " + " ");
                    }
                }
            }
            System.out.println(rightBlock);
        }
        System.out.println(wall);
        System.out.println("Количество жизней:\t" + live + "\n");
    }
}

// Вспомогательный класс для хранения координат (замена java.awt.Point, чтобы не было зависимостей)
class Point {
    int x, y;
    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}