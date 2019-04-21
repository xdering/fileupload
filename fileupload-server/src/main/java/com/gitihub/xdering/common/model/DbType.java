package com.gitihub.xdering.common.model;

public enum DbType {
	/**
     * mysql DB
     */
    MYSQL("com.mysql.jdbc.Driver"),
    /**
     * drds DB
     */
    DRDS("com.mysql.jdbc.Driver"),
    /**
     * oracle DB
     */
    ORACLE("oracle.jdbc.driver.OracleDriver"), SqlServer("com.microsoft.sqlserver.jdbc.SQLServerDriver");

	private String driver;

	DbType(String driver) {
		this.driver = driver;
	}

	public String getDriver() {
		return driver;
	}

	public boolean isMysql() {
		return this.equals(DbType.MYSQL);
	}

	public boolean isDRDS() {
		return this.equals(DbType.DRDS);
	}

	public boolean isOracle() {
		return this.equals(DbType.ORACLE);
	}

	public boolean isSqlServer() {
		return this.equals(DbType.SqlServer);
	}
}
