#!/bin/bash

export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export JDBC_URL='jdbc:mysql://db.labthreesixfive.com/swajahat?autoReconnect=true&useSSL=false'
export JDBC_USER='swajahat'
export JDBC_PW='CSC365-F2019_014387785'

javac DBTablePrinter.java
javac InnReservations.java
java InnReservations

