import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InnReservations {
    private Connection conn;

    private static final int rs_width = 150;

    public InnReservations() {
        try {
            conn = DriverManager.getConnection(System.getenv("JDBC_URL"),
                    System.getenv("JDBC_USER"),
                    System.getenv("JDBC_PW"));
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            System.out.println("Connection failed.");
        }
    }

    public static void main(String[] args) throws SQLException {
        boolean quit = false;

        InnReservations ir = new InnReservations();

        while (!quit) {
            String selection = getUserSelection();
            // get user selection from menu

            try {
                if (selection.toLowerCase().equals("rooms and rates") || selection.equals("1")) {
                    ir.fr1(); // fr1 done (Julian)
                } else if (selection.toLowerCase().equals("reservations") || selection.equals("2")) {
                    ir.fr2();
                } else if (selection.toLowerCase().equals("reservations change") || selection.equals("3")) {
                    ;
                } else if (selection.toLowerCase().equals("reservation cancellation") || selection.equals("4")) {
                    ;
                } else if (selection.toLowerCase().equals("detailed reservation information") || selection.equals("5")) {
                    ;
                } else if (selection.toLowerCase().equals("revenue") || selection.equals("6")) {
                    ;
                } else if (selection.toLowerCase().equals("exit") || selection.equals("7")) {
                    System.out.println("Thank you for using the Inn Reservation System. Goodbye.");
                    quit = true;
                } else {
                    System.out.println("Unknown selection. Please try again.");
                }
            } catch (SQLException e) {
                System.err.println("SQLException: " + e.getMessage());
            }
        }

        if (ir.conn != null) {
            ir.conn.close();
        }
    }

    public boolean checkDateValid(String strDate, String dateFormat){
        if (strDate == null)
            return false;

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setLenient(false);

        try {
            // if date is not valid, it will throw a ParseException
            Date date = sdf.parse(strDate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public static String getUserSelection() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please select an option from below.");
        System.out.println("1. Rooms and Rates");
        System.out.println("2. Reservations");
        System.out.println("3. Reservations Change");
        System.out.println("4. Reservation Cancellation");
        System.out.println("5. Detailed Reservation Information.");
        System.out.println("6. Revenue");
        System.out.println("7. Exit");
        System.out.print("Please enter your selection here: ");

        return scanner.nextLine();
    }

    private void fr1() throws SQLException {
        // Step 2: Construct SQL statement
        String sql = "select A.roomcode, A.roomname, A.beds, A.bedtype, A.maxocc, A.baseprice, A.decor,"
                    + " A.popScore, B.nextAvailableCheckin, C.latestStay, C.lengthOfStay"
                    + " from"
                    + " ("
                    + " (select roomcode, roomname, beds, bedtype, maxOcc, baseprice, decor,"
                    + " round(sum(t.occupiedDays)/180, 2) as popScore"
                    + " from ("
                    + " (select room, datediff("
                    + " if(checkout > curdate(), curdate(), checkout),"
                    + " if(checkin < date_add(curdate(), interval -180 day), date_add(curdate(), interval -180 day), checkin)"
                    + " ) occupiedDays"
                    + " from jdavi104.lab7_reservations"
                    + " where checkout > date_add(curdate(), interval -180 day) and checkin < curdate()"
                    + " ) t"
                    + " join jdavi104.lab7_rooms on t.room = roomcode"
                    + " )"
                    + " group by t.room"
                    + " order by popScore desc) A,"
                    + " (select distinct Room, CURRENT_DATE() as nextAvailableCheckin"
                    + " from jdavi104.lab7_reservations"
                    + " where Room not in ("
                    + " select Room"
                    + " from jdavi104.lab7_reservations"
                    + " where NOW() between Checkin and CheckOut"
                    + " )"
                    + " union"
                    + " select Room, CheckOut"
                    + " from ("
                    + " select *, RANK() OVER (partition by ROOM order by NextDate asc) as Ranking"
                    + " from ("
                    + " select *, DATEDIFF(CheckOut, NOW()) as NextDate"
                    + " from jdavi104.lab7_reservations as R"
                    + " where CODE not in ("
                    + " select R.CODE"
                    + " from jdavi104.lab7_reservations as R"
                    + " JOin jdavi104.lab7_reservations as R1 ON R.Room = R1.Room and R.CheckOut = R1.Checkin"
                    + " )"
                    + " and Room in ("
                    + " select Room"
                    + " from jdavi104.lab7_reservations"
                    + " where NOW() between Checkin and CheckOut)) as CheckOuts"
                    + " where NextDate > 0) as Ranker"
                    + " where Ranking = 1) B,"
                    + " (select room, checkout as latestStay, datediff(checkout, checkin) as lengthOfStay"
                    + " from jdavi104.lab7_reservations"
                    + " where (room, checkout) in ("
                    + " select room, max(checkout)"
                    + " from jdavi104.lab7_reservations"
                    + " where checkout < curdate()"
                    + " group by room)"
                    + " ) C"
                    + " )"
                    + " where A.roomcode = B.room and A.roomcode = C.room and B.room = C.room;";

        // Step 3: (omitted in this example) Start transaction
        // Step 4: Send SQL statement to DBMS
        try (Statement stmt = conn.createStatement()) {

            // Step 5: Receive results
            ResultSet rs = stmt.executeQuery(sql);
            DBTablePrinter.printResultSet(rs);
        }
        // Step 6: (omitted in this example) Commit or rollback transaction
        // Step 7: Close connection (handled by try-with-resources syntax)
    }

    private void fr2() throws SQLException {
        // TODO: set transaction boundaries
        // check if there are no rooms available, find similar rooms if so
        // reserve the room
        Scanner scanner = new Scanner(System.in);
        String firstName, lastName, roomCode, bedType, checkIn, checkOut;
        int numChildren = 0, numAdults = 0;
        boolean valid = true, noAvailableRooms = false;


        System.out.println("Please provide your: ");
        System.out.print("First Name: ");
        firstName = scanner.nextLine();

        System.out.print("Last Name: ");
        lastName = scanner.nextLine();

        System.out.print("Room Code Desired (or 'ANY' to indicate no preference): ");
        roomCode = scanner.nextLine();

        System.out.print("Bed Type Desired (or 'ANY' to indicate no preference): ");
        bedType = scanner.nextLine();

//        System.out.printf("Checking if date %s is valid...\n", checkIn);
//        System.out.println(checkDateValid(checkIn, "yyyy-MM-dd"));

        System.out.print("Check In Date (in the form 'yyyy-mm-dd'): ");
        checkIn = scanner.nextLine();
        do {
            while (!checkDateValid(checkIn, "yyyy-MM-dd")) {
                valid = false;
                System.out.printf("\"%s\" is not a valid date.\n", checkIn);
                System.out.print("Check In Date (in the form 'yyyy-mm-dd'): ");
                checkIn = scanner.next();
            }
            if (valid == false) {
                valid = true;
                scanner.nextLine(); // clears input buffer
            }
        } while (valid == false);

        System.out.print("Check Out Date (in the form 'yyyy-mm-dd'): ");
        checkOut = scanner.nextLine();
        do {
            while (!checkDateValid(checkOut, "yyyy-MM-dd")) {
                valid = false;
                System.out.printf("\"%s\" is not a valid date.\n", checkOut);
                System.out.print("Check Out Date (in the form 'yyyy-mm-dd'): ");
                checkOut = scanner.next();
            }
            if (valid == false) {
                valid = true;
                scanner.nextLine(); // clears input buffer
            }
        } while (valid == false);

        System.out.print("Number of Children: ");
        do {
            while (!scanner.hasNextInt()) {
                String input = scanner.next();
                System.out.printf("\"%s\" is not a valid number.\n", input);
                System.out.print("Number of Children: ");
            }
            numChildren = scanner.nextInt();
        } while (numChildren < 0);

        System.out.print("Number of Adults: ");
        do {
            while (!scanner.hasNextInt()) {
                String input = scanner.next();
                System.out.printf("\"%s\" is not a valid number.\n", input);
                System.out.print("Number of Adults: ");
            }
            numAdults = scanner.nextInt();
        } while (numAdults < 0);

        try (Statement stmt = conn.createStatement()) {
            // create TOTAL_WEEKDAYS function used when reserving a room to calculate total cost.
            // TOTAL_WEEKDAYS function from https://stackoverflow.com/questions/18302181/mysql-function-to-count-days-between-2-dates-excluding-weekends
            boolean exRes = stmt.execute("DROP FUNCTION IF EXISTS total_weekdays;");
            String ddl = "CREATE FUNCTION TOTAL_WEEKDAYS(date1 DATE, date2 DATE)\n" +
                    "RETURNS INT\n" +
                    "RETURN ABS(DATEDIFF(date2, date1)) + 1\n" +
                    "     - ABS(DATEDIFF(ADDDATE(date2, INTERVAL 1 - DAYOFWEEK(date2) DAY),\n" +
                    "                    ADDDATE(date1, INTERVAL 1 - DAYOFWEEK(date1) DAY))) / 7 * 2\n" +
                    "     - (DAYOFWEEK(IF(date1 < date2, date1, date2)) = 1)\n" +
                    "     - (DAYOFWEEK(IF(date1 > date2, date1, date2)) = 7);";

            exRes = stmt.execute(ddl);

            String getMaxOccSQL = "select max(maxOcc) from jdavi104.lab7_rooms;";
            ResultSet rs = stmt.executeQuery(getMaxOccSQL);
            int maxOcc = -1;
            while (rs.next()) {
                maxOcc = rs.getInt(1);
            }

            if (numChildren + numAdults > maxOcc) {
                System.out.printf("There are no rooms large enough for the number of people you entered (%d).\n", numChildren + numAdults);
                System.out.println("You must make multiple reservations.");
                return;
            }

        }

//        String sql = "SELECT DISTINCT RoomCode, bedType, maxOcc, basePrice, \n" +
//                "    RoomName, ? AS checkIn, \n" +
//                "    ? AS checkOut, \n" +
//                "    round(((TOTAL_WEEKDAYS(?, ?) * basePrice)\n" +
//                "    + ((datediff(?,?) - \n" +
//                "    TOTAL_WEEKDAYS(?, ?)) * basePrice * 1.1)) * 1.18,2)\n" +
//                "    AS TotalCost \n" +
//                "FROM jdavi104.lab7_rooms \n" +
//                "WHERE RoomCode LIKE (?) AND (bedType LIKE (?) AND \n" +
//                "maxOcc >= 3 AND RoomCode NOT IN \n" +
//                "(SELECT room FROM jdavi104.lab7_reservations \n" +
//                "WHERE ? < checkOut AND ? > checkIn))\n" +
//                "order by roomcode;";

        String sql = "SELECT DISTINCT RoomCode, bedType, maxOcc, basePrice, \n" +
                "    RoomName, ? AS checkIn, \n" +
                "    ? AS checkOut, \n" +
                "    round(((TOTAL_WEEKDAYS(?, ?) * basePrice)\n" +
                "    + ((datediff(?,?) - \n" +
                "    TOTAL_WEEKDAYS(?, ?)) * basePrice * 1.1)) * 1.18,2)\n" +
                "    AS TotalCost \n" +
                "FROM jdavi104.lab7_rooms \n" +
                "WHERE RoomCode LIKE (?) AND (bedType LIKE (?) AND \n" +
                "maxOcc >= 4 AND RoomCode NOT IN \n" +
                "(SELECT room FROM jdavi104.lab7_reservations \n" +
                "WHERE ? < checkOut AND ? > checkIn))\n" +
                "order by TotalCost;";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
            pstmt.setDate(3, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(4, java.sql.Date.valueOf(checkOut));
            pstmt.setDate(5, java.sql.Date.valueOf(checkOut));
            pstmt.setDate(6, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(7, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(8, java.sql.Date.valueOf(checkOut));

            if (roomCode.equalsIgnoreCase("any")) {
                pstmt.setString(9, "%");
            } else {
                pstmt.setString(9, roomCode);
            }

            if (bedType.equalsIgnoreCase("any")) {
                pstmt.setString(10, "%");
            } else {
                pstmt.setString(10, bedType);
            }

            pstmt.setDate(11, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(12, java.sql.Date.valueOf(checkOut));

            try (ResultSet rs = pstmt.executeQuery()) {
                DBTablePrinter.printResultSet(rs);
//                if (rs.next() == false) {
//                    noAvailableRooms = true;
//                }
//                else {
//                    rs.first();
//                    DBTablePrinter.printResultSet(rs);
//                }
//
//                if (noAvailableRooms) {
//                    System.out.println("No available rooms!");
//                }
            }


        }

        return;



        // Step 5: Receive results
//            ResultSet rs = stmt.executeQuery(sql);
//            DBTablePrinter.printResultSet(rs);

        // first check if any room is big enough for the reservation.

//        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setInt(1, numChildren + numAdults);
//
//            try (ResultSet rs = pstmt.executeQuery()) {
//                if (rs.next() == false) {
//                    System.out.println("No rooms can accomodate that amount of people. You must make multiple reservations.");
//                }
//            }
//
//        }
//
//        return;

        // Step 6: (omitted in this example) Commit or rollback transaction

    }
}

