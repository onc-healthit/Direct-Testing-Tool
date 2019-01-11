/**
 This software was developed at the National Institute of Standards and Technology by employees
of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
United States Code this software is not subject to copyright protection and is in the public domain.
This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
modified freely provided that any derivative works bear some notice that they are derived from it, and any
modified versions bear some notice that they have been modified.

Project: NWHIN-DIRECT
Authors: William Majurski
		 Frederic de Vaulx
		 Diane Azais
		 Julien Perugini
		 Antoine Gerardin
		
 */

package gov.nist.healthcare.ttt.direct.certificates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

public class PrivateCertificateLoader {

	private static Logger logger = Logger.getLogger(PrivateCertificateLoader.class.getName());
	
	String keyAlias = "";
	X509Certificate cert;
	PrivateKey pkey;
	Certificate[] chain;
	String signDN;
	String digestAlgo = "SHA1withRSA";

	public PrivateCertificateLoader(InputStream certificate, String password, String digestAlgo) throws Exception {

		Security.addProvider(new BouncyCastleProvider());

		// Set digest algorithm

		if(digestAlgo != null && !digestAlgo.equals("")) {
			this.digestAlgo = digestAlgo;
		}
		
		// Copy InputStream in byte array so we can read it more than once
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(certificate, baos);
		byte[] bytesCert = baos.toByteArray();
		
		//
		// Open the key store
		//
		KeyStore ks = null;
		try {
			ks = KeyStore.getInstance("PKCS12", "BC");
		} catch (KeyStoreException e1) {
			throw new KeyStoreException();
		} catch (NoSuchProviderException e1) {
			throw new NoSuchAlgorithmException();
		}

		try {
			if (password == null) {
				password = "";
			}
			ks.load(new ByteArrayInputStream(bytesCert), password.toCharArray());
		} catch (Exception e1) {

			logger.error("File is not a private certificate. Trying to read as a public certificate");
			try {
				@SuppressWarnings("unused")
				PublicCertLoader publicCertLoader = new PublicCertLoader(new ByteArrayInputStream(bytesCert));
				// System.out.println(encCert);
			} catch (Exception e2) {
				logger.warn("File is not a public certificate");
				throw new Exception("The file is not a certificate. You need to upload a private certificate in order to decrypt the message");
			}
			logger.warn("File is a public certificate");
			throw new Exception("The file is a public certificate. You need to upload a private certificate in order to decrypt the message");
			
		}

		@SuppressWarnings("rawtypes")
		Enumeration e = null;
		try {
			e = ks.aliases();
		} catch (KeyStoreException e1) {
			throw new KeyStoreException();
		}

		this.keyAlias = null;

		if (e != null) {
			while (e.hasMoreElements()) {
				String alias = (String) e.nextElement();

				try {
					if (ks.isKeyEntry(alias)) {
						keyAlias = alias;
					}
				} catch (KeyStoreException e1) {
					throw new KeyStoreException();
				}
			}
		}

		if (keyAlias == null) {
			throw new Exception("Cannot find an alias");
		}

		chain = ks.getCertificateChain(keyAlias);

		//
		// cert that issued the signing certificate
		//
		signDN = ((X509Certificate) chain[0]).getIssuerDN().toString();

		//
		// find the certificate for the private key and generate a
		// suitable recipient identifier.
		//

		this.cert = null;

		try {
			cert = (X509Certificate) ks.getCertificate(keyAlias);
			// System.out.println(cert);
		} catch (KeyStoreException e1) {
			throw new KeyStoreException();
		}

		pkey = (PrivateKey) ks.getKey(keyAlias, null);

	}

	public SMIMESignedGenerator getSMIMESignedGenerator()
			throws CertificateEncodingException, OperatorCreationException {
		Collection<X509Certificate> signingCertificates = new ArrayList<X509Certificate>();
		X509CertificateEx signCert = X509CertificateEx.fromX509Certificate(
				(X509Certificate) this.getChain()[0], this.getPrivateKey());

		// System.out.println(signCert);

		signingCertificates.add(signCert);

		//
		// create a CertStore containing the certificates we want carried
		// in the signature
		//
		Store certs = new JcaCertStore(signingCertificates);

		//
		// create some smime capabilities in case someone wants to respond
		//
		ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
		SMIMECapabilityVector caps = new SMIMECapabilityVector();

		caps.addCapability(SMIMECapability.dES_EDE3_CBC);
		caps.addCapability(SMIMECapability.rC2_CBC, 128);
		caps.addCapability(SMIMECapability.dES_CBC);
		caps.addCapability(new ASN1ObjectIdentifier("1.2.840.113549.1.7.1"));
		caps.addCapability(new ASN1ObjectIdentifier("1.2.840.113549.1.9.22.1"));

		signedAttrs.add(new SMIMECapabilitiesAttribute(caps));

		// logger.debug("Signing Cert is \n = " + signCert.toString());
		//
		// add an encryption key preference for encrypted responses -
		// normally this would be different from the signing certificate...
		//
		IssuerAndSerialNumber issAndSer = new IssuerAndSerialNumber(
				new X500Name(this.getSignDN()), signCert.getSerialNumber());

		signedAttrs.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer));

		//
		// create the generator for creating an smime/signed message
		//
		SMIMESignedGenerator gen = new SMIMESignedGenerator();

		//
		// add a signer to the generator - this specifies we are using SHA1 and
		// adding the smime attributes above to the signed attributes that
		// will be generated as part of the signature. The encryption algorithm
		// used is taken from the key - in this RSA with PKCS1Padding
		//
		logger.info("Creating message with algorithm " + digestAlgo);

		gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
				.setProvider("BC")
				.setSignedAttributeGenerator(new AttributeTable(signedAttrs))
				.build(digestAlgo, signCert.getPrivateKey(), signCert));

		//
		// add our pool of certs and cerls (if any) to go with the signature
		//
		gen.addCertificates(certs);

		return gen;
	}

	public String getKeyAlias() {
		return this.keyAlias;
	}

	public X509Certificate getX509Certificate() {
		return this.cert;
	}

	public PrivateKey getPrivateKey() {
		return this.pkey;
	}

	public Certificate[] getChain() {
		return this.chain;
	}

	public String getSignDN() {
		return this.signDN;
	}

}
