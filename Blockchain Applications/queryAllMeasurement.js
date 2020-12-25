// restrict youself from adding console.log command here as the associated python code will fail to parse unexpected
// stdout values

'use strict';

// Bring key classes into scope, most importantly Fabric SDK network class
const fs = require('fs');
const yaml = require('js-yaml');
const { FileSystemWallet, Gateway } = require('fabric-network');
// Only the admin user should have the capability to query the functionality
let aggregatorProfile = yaml.safeLoad(fs.readFileSync('config.yaml', 'utf8'));
const channel_name = aggregatorProfile['ChannelName'][0];;
const wallet = new FileSystemWallet('../identity/wallet');

async function main() {

    // A gateway defines the peers used to access Fabric networks
    const gateway = new Gateway();

    // Main try/catch block
    try {

        // Specify userName for network access
        const userName = 'admin';

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
        const results = await contract.submitTransaction('queryAll');
        // const results = await contract.submitTransaction('queryWithQueryString','EN4103327-Linux');
// Python can only get the data from JS through stdout, hence the values are being iteratively printed using console.log
        let resultsObject =JSON.parse(results);
        let output_data = {};
        // console.log(resultsObject);
        resultsObject.forEach((result)=> {
            process.stdout.write(result['Measurement']['element']+'::'+result['Measurement']['time_stamp']+'::'+result['Measurement']['measurement']+"|")});
            
    } catch (error) {

        // console.log(`Error processing transaction. ${error}`);
        console.log(error.stack);

    } finally {

        gateway.disconnect();

    }
}
main().then(() => {
// Avoid priting any more message as python recieves everything as stdout
    // console.log('Issue program complete.');

}).catch((e) => {

    // console.log('Issue program exception.');
    console.log(e);
    console.log(e.stack);
    process.exit(-1);

});
