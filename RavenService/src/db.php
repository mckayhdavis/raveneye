<?php

class Database {
	
	function __construct() {
		echo 'hi!';
	}

	public static function connect() {
		/* connect to the db */
		$link = mysql_connect('10.6.186.123','mdupls','SouthAfr1ca') or die('Cannot connect to the DB');
		mysql_select_db('mdupls',$link) or die('Cannot select the DB');
		
		return $link;
	}

}

?>