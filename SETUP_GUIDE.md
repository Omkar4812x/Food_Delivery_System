# FoodDeliverySystem - Complete Setup Guide

This guide contains everything you need to set up, configure, and run the Food Delivery System on a new machine. It relies upon Java 17, Apache Tomcat 10.1, and MySQL.

## 1. Prerequisites

Before starting, ensure the following software is installed on the machine:
- **Java Development Kit (JDK):** Version 17 (Required for modern compilation).
- **Eclipse IDE:** Specifically **"Eclipse IDE for Enterprise Java and Web Developers"**.
- **Apache Tomcat:** Version 10.1 (Extract the Tomcat zip folder somewhere accessible on your drive).
- **MySQL Server & MySQL Workbench:** MySQL 8 is recommended.

---

## 2. Database Configuration

1. Open **MySQL Workbench** and connect to your local MySQL instance using the username `root` and password `root` (If your credentials differ, you must update `src/main/java/edu/omkar/dbconfig/DBConfig.java` to match).
2. Go to `File -> Run SQL Script...` or simply open a new query tab.
3. Select the `food_delivery_db.sql` file located in the root of the project directory.
4. Run the script. This will automatically:
   - Create the `food_delivery_db` database.
   - Set up all required tables (`users`, `restaurants`, `food_items`, `orders`, `order_items`).
   - Insert the default Administrator credentials.

---

## 3. Importing Project into Eclipse

1. Launch Eclipse.
2. Navigate to **File -> Import...**.
3. Expand the **Maven** folder and select **Existing Maven Projects**, then click **Next**.
4. Browse to the root folder of `FoodDeliverySystem` and select it.
5. Ensure the `pom.xml` checkbox is ticked in the `Projects` section and click **Finish**.
6. Wait for Eclipse to finish building the workspace and downloading Maven dependencies (Watch the progress bar at the bottom right).

---

## 4. Configuring Apache Tomcat 10.1

1. Open the **Servers** tab at the bottom panel of Eclipse (If it's not visible, go to `Window -> Show View -> Servers`).
2. Click **"No servers are available. Click this link to create a new server..."**
3. Expand **Apache** and select **Tomcat v10.1 Server**. Click **Next**.
4. Click **Browse...** and select the folder where you extracted Apache Tomcat 10.1, then click **Next**.
5. You will see `FoodDeliverySystem` on the left under "Available:". Select it and click **Add >** to move it to "Configured:".
6. Click **Finish**.

---

## 5. Running the Application

1. In your **Servers** tab, select the newly created Tomcat 10.1 server.
2. Click the green **Start** icon (or right-click the server -> `Start`). 
3. Verify the console logs to ensure there are no startup errors.
4. Once the server says `[Started, Synchronized]`, open your web browser.
5. Go to: **`http://localhost:8080/FoodDeliverySystem/login.html`** *(Make sure the port matches your Tomcat port, typically 8080)*

---

## 6. Access Profiles

**Administrator Access (Manage Restaurants, Menus, Users & View Orders):**
- **Email:** `admin@food.com`
- **Password:** `admin123`

**Customer Access (Placing Orders):**
You can create a brand new user account directly on the login page via the "Sign up" tab to test placing live orders as a customer.

---

### Troubleshooting

- **404 Errors or White Screens:** Right-click the project -> **Maven -> Update Project...** (Check "Force Update of Snapshots/Releases" and click OK). Then right-click the server -> **Clean...** and Restart.
- **Database Connection Failed:** Verify that MySQL is running on port 3306 and that your `DBConfig.java` has the correct SQL password for the new machine.
- **Java Compilation Errors:** Ensure Eclipse recognizes JDK 17. Right-click the project -> **Properties -> Java Build Path -> Libraries** and make sure "JRE System Library" is pointing to JavaSE-17.
