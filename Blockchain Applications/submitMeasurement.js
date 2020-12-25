'use strict';

// Bring key classes into scope, most importantly Fabric SDK network class
const fs = require('fs');
const yaml = require('js-yaml');
const { FileSystemWallet, Gateway } = require('fabric-network');
// In the event of a REST API, the prosumer will not have access to the following file, this is only availbale for development purpose
// Retrieving the inputs from python script
// const function_value = process.argv[2];
const uname= process.argv[2];
const time_stamp= process.argv[3];
const sensor_data = process.argv[4];
const Measurement = require('../../Agg0/contract/lib/measurement.js');
let aggregatorProfile = yaml.safeLoad(fs.readFileSync('config.yaml', 'utf8'));
const channel_name = aggregatorProfile['ChannelName'][0];
const wallet = new FileSystemWallet('../identity/wallet');

//console.log(`The Prosumer data::prosumer: ${prosumer}, Budget: ${budget}, Parameters: ${parameters} \n`)
// Main program function
async function main() {

    // A gateway defines the peers used to access Fabric networks
    const gateway = new Gateway();

    // Main try/catch block
    try {

        // Specify userName for network access
        const userName = uname;

        // Load connection profile; will be used to locate a gateway
        let connectionProfile = yaml.safeLoad(fs.readFileSync('../gateway/networkConnection.yaml', 'utf8'));

        // Set connection options; identity and wallet
        let connectionOptions = {
            identity: userName,
            wallet: wallet,
            discovery: { enabled:false, asLocalhost: true }
        };

        // Connect to gateway using application specified parameters
        await gateway.connect(connectionProfile, connectionOptions);

        const network = await gateway.getNetwork(channel_name);
        const contract = await network.getContract('measurementcontract', 'org.papernet.measurement');
        // Initiate the transaction
        // console.log('Prosumer While Creating Bid:' + prosumer);
        // console.log(all_measurement_data[measurement_id]);
        const issueResponse = await contract.submitTransaction('createMeasurement', uname, time_stamp,sensor_data );
        let measurement = Measurement.fromBuffer(issueResponse);
        console.log(`Submitted by user: ${uname} ==:${measurement.measurement} at timestamp : ${time_stamp}`);
        
        
        
        
        

    } catch (error) {

        console.log(`Error processing transaction. ${error}`);
        console.log(error.stack);

    } finally {
        // Disconnect from the gateway
        gateway.disconnect();
    }
}
main().then(() => {

    // console.log('Submitted Bid Successfully.');

}).catch((e) => {

    // console.log('Exception happened while submitting the bid.');
    // console.log(e);
    // console.log(e.stack);
    process.exit(-1);

});
