<?php
require_once("db.php");

?>

<!DOCTYPE html>
<html dir="ltr" lang="en-US">
<head>
<title>Add Place</title>
</head>
<body>







<?php

$success = false;

if(isset($_POST['name']) && isset($_POST['description'])) {
	$name = $_POST['name'];
	$description = $_POST['description'];
	$code = $_POST['code'];

	/* connect to the db */
	$link = Database::connect();

	$query = sprintf("INSERT INTO Places (Name, Description) VALUES ('%s', '%s')", mysql_real_escape_string(addslashes($name)), mysql_real_escape_string(addslashes($description)));
	$result = mysql_query($query,$link) or die(mysql_error());

	echo $result;

	if($result) {
		$id = mysql_insert_id();
		if($code != "") {
			$query = sprintf("INSERT INTO BuildingCodes (PlaceID, Code) VALUES ('%s', '%s')", mysql_real_escape_string($id), mysql_real_escape_string($code));
			$result = mysql_query($query,$link) or die(mysql_error());
		}
	}
}
?>



	<form action="addplace.php" method="post">
		<p>Name</p>
		<p>
			<input type="text" name="name" value="<?php echo $name; ?>" />
		</p>
		<p>Description</p>
		<p>
			<input type="text" name="description"
				value="<?php echo $description; ?>" />
		</p>
		<p>Building Code</p>
		<p>
			<input type="text" name="code" value="<?php echo $code; ?>" />
		</p>
		<p>
			<input type="submit" value="Add" />
		</p>
	</form>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	<?php 
	if($result) {
		echo "Success!";
	} else {
		echo "Failed";
	}
	?>

</body>
</html>
