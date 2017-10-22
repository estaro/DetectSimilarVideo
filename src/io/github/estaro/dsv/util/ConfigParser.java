package io.github.estaro.dsv.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import io.github.estaro.dsv.bean.Config;

public class ConfigParser {

	private static final String CONFIG_FILE = "resource/config.xml";

	public static Config load() {
		try {
			XMLDecoder decorder = new XMLDecoder(
					new BufferedInputStream(
							new FileInputStream(CONFIG_FILE)));
			Config config = (Config) decorder.readObject();
			decorder.close();
			return config;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return new Config();
	}

	public static void save(Config config) {
		try {
			XMLEncoder encoder = new XMLEncoder(
					new BufferedOutputStream(
							new FileOutputStream(CONFIG_FILE)));
			encoder.writeObject(config);
			encoder.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
