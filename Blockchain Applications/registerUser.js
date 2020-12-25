/*
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';
const yaml = require('js-yaml');
const fs = require('fs');
const uname= process.argv[2];
const user_value= process.argv[3];
const { FileSystemWallet, Gateway, X509WalletMixin } = require('fabric-network');
const path = require('path');
let aggregatorProfile = yaml.safeLoad(fs.readFileSync('config.yaml', 'utf8'));
const ccpPath = path.resolve('connection-agg'+String(aggregatorProfile['AggID'][0])+'.json');

async function main() {
    try {

        // Create a new file system based wallet for managing identities.
        const walletPath = path.resolve(process.cwd(),'..','identity','wallet');
        const wallet = new FileSystemWallet(walletPath);
        console.log(`Wallet path: ${walletPath}`);

        // Check to see if we've already enrolled the user.
        const userExists = await wallet.exists(uname);
        if (userExists) {
            console.log(`An identity for the user ${uname} already exists in the wallet`);
            return;
        }

        // Check to see if we've already enrolled the admin user.
        const adminExists = await wallet.exists('admin');
        if (!adminExists) {
            console.log('An identity for the admin user "admin" does not exist in the wallet');
            console.log('Run the enrollAdmin.js application before retrying');
            return;
        }

        // Create a new gateway for connecting to our peer node.
        const gateway = new Gateway();
        await gateway.connect(ccpPath, { wallet, identity: 'admin', discovery: { enabled: true, asLocalhost: true } });

        // Get the CA client object from the gateway for interacting with the CA.
        const ca = gateway.getClient().getCertificateAuthority();
        const adminIdentity = gateway.getCurrentIdentity();
	//console.log(adminIdentity);

        // Register the user, enroll the user, and import the new identity into the wallet.
        const secret = await ca.register({ affiliation: '', enrollmentID: uname, role: 'client', attrs : [{name: 'asset', value: user_value, ecert: true}]}, adminIdentity);
        const enrollment = await ca.enroll({ enrollmentID: uname, enrollmentSecret: secret });

        const userIdentity = X509WalletMixin.createIdentity('Agg'+String(aggregatorProfile['AggID'][0])+'MSP', enrollment.certificate, enrollment.key.toBytes());
        // const userIdentityLocation = path.resolve(process.cwd(),'..','identity','User',uname)
        await wallet.import(uname, userIdentity);
        console.log(`Successfully registered and enrolled prosumer ${uname} and imported it into the wallet`);

    } catch (error) {
        console.error(`Failed to register user ${uname}: ${error}`);
        process.exit(1);
    }
}

main();
