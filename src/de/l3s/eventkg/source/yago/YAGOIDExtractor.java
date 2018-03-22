package de.l3s.eventkg.source.yago;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class YAGOIDExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public YAGOIDExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("YAGOIdExtractor", Source.YAGO, "Extracts YAGO IDs using Wikidata IDs.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {

		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.YAGO_WIKIDATA_INSTANCES);

			String line;
			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("#") || line.startsWith("@"))
					continue;
				String[] parts = line.split(Config.TAB);

				if (!parts[1].equals("owl:sameAs"))
					continue;

				if (!parts[2].startsWith("<http://www.wikidata.org/entity/"))
					continue;

				String yagoId = parts[0].substring(1, parts[0].length() - 1);
				String wikidataId = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].lastIndexOf(">"));

				Entity entity = this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(wikidataId);
				if (entity != null)
					entity.setYagoId(yagoId);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
