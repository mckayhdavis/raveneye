<?php
require_once(realpath($_SERVER["DOCUMENT_ROOT"]).'/michael/headers/setup.php');
protect_page();

$defselect = "Select...";
$error = false;
$invalid_email = false;
	
$serial = addslashes($_SESSION['serial']);
$problem = addslashes($_SESSION['problem']);
//If there is no serial number, user has entered this page without starting
//at index.php
if(!$serial || !$problem) {
	header("Location: index.php");
	exit;
}

db_connect();

$userID = $_SESSION['userID'];

//Error check when submitting form.
if($_GET["sbm"] == 2) {		
	$case = addslashes(trim($_SESSION['case']));	
	$origPO = addslashes(trim($_SESSION['origPO']));
	$repPO = addslashes(trim($_SESSION['repPO']));
	
	$priority = addslashes(trim($_SESSION['priority']));
	$warranty = addslashes(trim($_SESSION['warranty']));
	$reason = addslashes(trim($_SESSION['reason']));
	
	$cust = addslashes(trim($_POST['cust']));
	$contact = addslashes(trim($_POST['contact']));
	$email = addslashes(trim($_POST['email']));
	$phone = addslashes(trim($_POST['phone']));
	$fax = addslashes(trim($_POST['fax']));
	
	$facility = addslashes(trim($_POST['facility']));
	$courier = addslashes(trim($_POST['courier']));
	$shipmethod = addslashes(trim($_POST['shipmethod']));
	$courieraccount = addslashes(trim($_POST['courieraccount']));
	$couriernotes = addslashes(trim($_POST['couriernotes']));
	$street = addslashes(trim($_POST['street']));
	$street2 = addslashes(trim($_POST['street2']));
	$street3 = addslashes(trim($_POST['street3']));
	$city = addslashes(trim($_POST['city']));
	$state = addslashes(trim($_POST['state']));
	$zip = addslashes(trim($_POST['zip']));
	$country = addslashes(trim($_POST['country']));

	$invalid_email = !valid_email($email);
	$error = (!$cust || !$contact || !$email || $invalid_email || !$street || !$city || !$state || !$zip || !$country);
	
	if(!$error) {
		unset($_SESSION['case']);
		unset($_SESSION['origPO']);
		unset($_SESSION['repPO']);
		unset($_SESSION['priority']);
		unset($_SESSION['warranty']);
		unset($_SESSION['reason']);
		
		//Submit entry into database.
		
		//RMA
		$query = "insert into RMA (UserID, CustID) values ('$userID', '$custID')";
		$result = mysql_query($query);
		$rma = mysql_insert_id();

		//Contact
		$query = "insert into RMAcontact (RMA, Name, Email, Phone, Fax) values ('$rma', '$contact', '$email', '$phone', '$fax')";
		$result = mysql_query($query);
		
		//actionID
		$query = "select * from action_bank where Action like '$reason' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$actionID = stripslashes($row['ActionID']);	
		}

		//priorityID
		$query = "select * from priority_bank where Priority like '$priority' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$priorityID = stripslashes($row['PriorityID']);	
		}
		
		//Reason for return
		$query = "insert into RMAreason (RMA, ActionID, PriorityID, Warranty, Problem) values ('$rma', '$actionID', '$priorityID', '$warranty', '$problem')";
		$result = mysql_query($query);
		
		//Address
		$query = "insert into address_bank (UserID, Street, Street2, Street3, City, State, ZIP, Country, Contact, Phone) values ('$userID','$street','$street2','$street3','$city','$state','$zip','$country','$contact','$phone')";
		$result = mysql_query($query);
		$adrID = mysql_insert_id();
		
		//Ship Method
		$query = "select * from shipmethods_bank where Method like '$shipmethod' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$methodID = stripslashes($row['MethodID']);		
		}

		//Courier
		$query = "select * from courier_bank where Courier like '$courier' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$courierID= stripslashes($row['CourierID']);		
		}
		
		$query = "insert into RMAdropship (RMA, Facility, Street, City, State, ZIP, Country) values ('$rma','$facility','$street.$street2.$street3','$city','$state','$zip','$country')";
		$result = mysql_query($query);
		
		session_register("rma");
		$_SESSION['rma'] = $rma;
		//Product

		$query = "select * from serial_bank where Serial like '$serial' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$partID = stripslashes($row['PartID']);		
		}
		$query = "select * from product_bank where PartID like '$partID' LIMIT 1";
		$result = mysql_query($query);
		
		$num_results = mysql_num_rows($result);
		if($num_results == 1) {
			$row = mysql_fetch_array($result);
			$product = stripslashes($row['Product']);		
		}
		
		$query = "insert into RMAreturns (RMA, PartID, Serial) values ('$rma', '$partID', '$serial')";
		$result = mysql_query($query);
		
		$_SESSION['rma'] = $rma;
		header("Location: view.php");
		exit;
	}
}
//Page visited without proper entry.
else if($_GET["sbm"] != 1) {
	unset($_SESSION['case']);
	unset($_SESSION['serial']);
	unset($_SESSION['origPO']);
	unset($_SESSION['repPO']);
	unset($_SESSION['priority']);
	unset($_SESSION['warranty']);
	unset($_SESSION['reason']);
	unset($_SESSION['problem']);
	
	//User has entered this page without starting at index.php
	header("Location: index.php");
	exit;	
} else {
	$result = mysql_query("SELECT * FROM users WHERE ID='$userID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$custID = stripslashes($row['CustID']);
		$contact = stripslashes($row['Firstname']).' '.stripslashes($row['Lastname']);
		$email = stripslashes($row['Email']);
	}
	$result = mysql_query("SELECT * FROM customer_bank WHERE CustID='$custID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$cust = stripslashes($row['Name']);
	}
	$result = mysql_query("SELECT * FROM user_address WHERE UserID='$userID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$street = stripslashes($row['Street']);
		$street2 = stripslashes($row['Street2']);
		$street3 = stripslashes($row['Street3']);
		$city = stripslashes($row['City']);
		$state = stripslashes($row['State']);
		$zip = stripslashes($row['Zip']);
		$country = stripslashes($row['Country']);
		$phone = stripslashes(trim($row['Phone']));
		$fax = stripslashes(trim($row['Fax']));
	}
	$result = mysql_query("SELECT * FROM user_courier_info WHERE UserID='$userID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$courierID = stripslashes($row['CourierID']);
		$shipmethodID = stripslashes($row['MethodID']);
		$courieraccount = stripslashes($row['Account']);
		$couriernotes = stripslashes($row['Notes']);
	}
	$result = mysql_query("SELECT * FROM courier_bank WHERE CourierID='$courierID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$courier = stripslashes($row['Name']);
	}
	$result = mysql_query("SELECT * FROM shipmethods_bank WHERE MethodID='$shipmethodID'");
	if(mysql_num_rows($result) == 1) {
		$row = mysql_fetch_array($result);
		
		$shipmethod = stripslashes($row['Name']);
	}
	
}

print_header($_SESSION['page_title'], $_SESSION['page_description']);

print_top_menu(); // Print the top menu
print_side_bar(); // Print the side bar

start_content(); // Open tags for the content pane

print_title($_SESSION['page_title'], $_SESSION['page_description']);

?>

<div class="toolbar"><div>
<h6>STEP: 3 of 3</h6>
</div></div><br />

<?php

if($error) {

?>

<div align="center">
<div class="box red_light" style="float:none"><div class="border">
<p><strong>One or more required fields have not been filled.</strong></p>
<p>Required fields are marked with an <span class="error">*</span></p>
</div></div>
</div><br />

<?php
}
?>
    
<form name="shippingRMA" action="shipping.php?sbm=2" method="POST">

<div class="box size2">
	
	<h3>Customer Information:</h3>
	
    <div class="field_box"><p><span class="error">*</span> Customer Name: </p>
    <input maxlength="40" type="text" name="cust" value="<?php echo $cust; ?>" <?php if(!$cust && $error) echo 'class="error"'; ?> />
    </div>
    
    <br /><br /><div class="hr"></div>
   
    <div class="field_box"><p><span class="error">*</span> Contact Name: </p>
    <input maxlength="40" type="text" name="contact" value="<?php echo $contact; ?>" <?php if(!$contact && $error) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p><span class="error">*</span> Email: <?php if($invalid_email) echo '<span class="error">(Invalid)</span>'; ?></p>
    <input maxlength="40" type="text" name="email" value="<?php echo $email; ?>" <?php if($invalid_email || (!$email && $error)) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p>Telephone: </p>
    <input maxlength="30" type="text" name="phone" value="<?php echo $phone; ?>" />
    </div>
    
    <div class="field_box"><p>Fax: </p>
    <input maxlength="30" type="text" name="fax" value="<?php echo $fax; ?>" />
    </div>
    
    <br /><div class="hr"></div>
    
    <div class="field_box"><p>Facility Name: <br /><span class="small">(If applicable)</span></p>
    <input maxlength="70" type="text" name="facility" value="<?php echo $facility; ?>" />
    </div>
    
</div>
<div class="box size2">
    
	<h3>Courier Information:</h3>
	
    <div class="field_box"><p>Preferred Courier:</p>
    <select name="courier">
    <option>Select...</option>
    <?php
	$query = "select * from courier_bank order by Courier";
	$result = mysql_query($query) or die(mysql_error());  
	
	while($row = mysql_fetch_array($result)) {
		$cur = stripslashes($row['Courier']);
		if($courier == $cur) echo '<option selected>'.$cur.'</option>';
		else echo '<option>'.$cur.'</option>';
	}
    ?>
    </select>   
    </div>
    
    <div class="field_box"><p>Preferred Shipping Method:</p>
    
    <select name="shipmethod">
    <option>Select...</option>
    <?php
	$query = "select * from shipmethods_bank order by Method";
	$result = mysql_query($query) or die(mysql_error());  

	while($row = mysql_fetch_array($result)) {
		$cur = stripslashes($row['Method']);
		if($shipmethod == $cur) echo '<option selected>'.$cur.'</option>';
		else echo '<option>'.$cur.'</option>';
	}
    ?>
    </select>    
    </div>
    
    <div class="field_box"><p>Notes:</p>
    <input maxlength="15" type="text" name="couriernotes" value="<?php echo $couriernotes; ?>" />
    </div>
    
    <div class="field_box"><p>Courier Account#:</p>
    <input maxlength="15" type="text" name="courieraccount" value="<?php echo $courieraccount; ?>" />
    </div>
    
    <br />
	<h3>Address Information:</h3>
    
    <div class="field_box"><p><span class="error">*</span> Street:</p>
    <input maxlength="70" type="text" name="street" value="<?php echo $street; ?>" <?php if(!$street && $error) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p></p>
    <input maxlength="40" type="text" name="street2" value="<?php echo $street2; ?>" />
    </div>
    
    <div class="field_box"><p></p>
    <input maxlength="40" type="text" name="street3" value="<?php echo $street3; ?>" />
    </div>
    
    <div class="field_box"><p><span class="error">*</span> City:</p>
    <input maxlength="40" type="text" name="city" value="<?php echo $city; ?>" <?php if(!$city && $error) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p><span class="error">*</span> State / Province:</p>
    <input maxlength="40" type="text" name="state" value="<?php echo $state; ?>" <?php if(!$state && $error) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p><span class="error">*</span> ZIP / Postal Code:</p>
    <input maxlength="10" type="text" name="zip" value="<?php echo $zip; ?>" <?php if(!$zip && $error) echo 'class="error"'; ?> />
    </div>
    
    <div class="field_box"><p><span class="error">*</span> Country:</p>
    <input maxlength="40" type="text" name="country" value="<?php echo $country; ?>" <?php if(!$country && $error) echo 'class="error"'; ?> />
	</div>
	
</div>

<div class="toolbar"><div><a href="info.php?sbm=3">Back</a>&nbsp;|&nbsp;<input type="submit" value="Finish" /></div></div>

</form>



<script type="text/javascript" language="JavaScript">

document.forms['shippingRMA'].elements['cust'].focus();

</script>



<?php
end_content(); // Close tags for the content pane
print_end_page();
?>