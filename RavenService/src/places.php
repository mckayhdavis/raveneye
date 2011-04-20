<?php
require_once("db.php");

/* soak in the passed variable or set our own */
$offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0; //10 is the default
$number_of_posts = isset($_GET['limit']) ? intval($_GET['limit']) : 10; //10 is the default
$format = strtolower($_GET['format']) == 'json' ? 'json' : 'xml'; //xml is the default
$content = strtolower($_GET['content']) == 'basic' ? 'basic' : 'extended'; //xml is the default
$place = $_GET['place'];

/* connect to the db */
$link = Database::connect();

$placeCriteria = $place != "" ? "Places.ID = $place" : "1";

/* grab the posts from the db */
switch($content) {
	case 'basic':
		$query = "SELECT ID, Places.Name as Name, Street, City, Provinces.Code as PCode, PostalCode, Country, BuildingCodes.Code as BCode, Latitude, Longitude, FileName, Caption, ReviewCount, Rating FROM (((((((Places NATURAL JOIN Cities) NATURAL JOIN Provinces) NATURAL JOIN Countries) LEFT JOIN BuildingCodes ON Places.ID = BuildingCodes.PlaceID) LEFT OUTER JOIN PlaceImage ON Places.ID = PlaceImage.PlaceID) LEFT OUTER JOIN Locations ON Places.ID = Locations.PlaceID) LEFT OUTER JOIN ((SELECT PlaceID, count(Reviews.PlaceID) as ReviewCount FROM Reviews) as ReviewsCount) ON Places.ID = ReviewsCount.PlaceID) WHERE $placeCriteria ORDER BY Places.Name ASC LIMIT $offset, $number_of_posts";
		break;
	case 'extended':
	default:
		$query = "SELECT ID, Places.Name as Name, Street, City, Provinces.Code as PCode, PostalCode, Country, Description, BuildingCodes.Code as BCode, Latitude, Longitude, FileName, Caption, ReviewCount, Rating FROM (((((((Places NATURAL JOIN Cities) NATURAL JOIN Provinces) NATURAL JOIN Countries) LEFT JOIN BuildingCodes ON Places.ID = BuildingCodes.PlaceID) LEFT OUTER JOIN PlaceImage ON Places.ID = PlaceImage.PlaceID) LEFT OUTER JOIN Locations ON Places.ID = Locations.PlaceID) LEFT OUTER JOIN ((SELECT PlaceID, count(Reviews.PlaceID) as ReviewCount FROM Reviews) as ReviewsCount) ON Places.ID = ReviewsCount.PlaceID) WHERE $placeCriteria ORDER BY Places.Name ASC LIMIT $offset, $number_of_posts";
}

$result = mysql_query($query,$link) or die(mysql_error());

/* create one master array of the records */
$posts = array();
if(mysql_num_rows($result)) {
	while($post = mysql_fetch_assoc($result)) {
		$post['Description'] = stripslashes($post['Description']);

		$posts[] = array('place'=>$post);
	}
}

/* output in necessary format */
if($format == 'json') {
	header('Content-type: application/json');
	echo json_encode(array('places'=>$posts));
}
else {
	header('Content-type: text/xml');
	echo '<places>';
	foreach($posts as $index => $post) {
		if(is_array($post)) {
			foreach($post as $key => $value) {
				echo '<',$key,'>';
				if(is_array($value)) {
					foreach($value as $tag => $val) {
						echo '<',$tag,'>',htmlentities($val),'</',$tag,'>';
					}
				}
				echo '</',$key,'>';
			}
		}
	}
	echo '</places>';
}

?>