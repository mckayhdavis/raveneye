<?php
require_once("db.php");

/* soak in the passed variable or set our own */
$offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0; //10 is the default
$number_of_posts = isset($_GET['limit']) ? intval($_GET['limit']) : 10; //10 is the default
$format = strtolower($_GET['format']) == 'json' ? 'json' : 'xml'; //xml is the default
$place = $_GET['place']; //no default

/* connect to the db */
$link = Database::connect();

$review_title = $_POST['title'];
if($review_title != "") {
	// Write a review.
	$review_body = $_POST['body'];

	$query = "INSERT INTO Reviews (PlaceID, Name, Text) VALUES ((SELECT ID FROM Places WHERE ID = $place), $review_title, $review_body)";
} else {
	// Pull reviews.

	/* grab the posts from the db */
	$query = "SELECT Date, Name, Text FROM Reviews WHERE PlaceID = $place ORDER BY Date ASC LIMIT $offset, $number_of_posts";
	$result = mysql_query($query,$link) or die(mysql_error());

	/* create one master array of the records */
	$posts = array();
	if(mysql_num_rows($result)) {
		while($post = mysql_fetch_assoc($result)) {
			$posts[] = array('review'=>$post);
		}
	}

	/* output in necessary format */
	if($format == 'json') {
		header('Content-type: application/json');
		echo json_encode(array('reviews'=>$posts));
	} else {
		header('Content-type: text/xml');
		echo '<reviews>';
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
		echo '</reviews>';
	}
}
?>