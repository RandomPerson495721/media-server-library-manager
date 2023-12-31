package dev.partin.james.jellyfinlibrarymanager;

import dev.partin.james.jellyfinlibrarymanager.api.model.JobDefinition;
import dev.partin.james.jellyfinlibrarymanager.helpers.TranscodeConfiguration;
import dev.partin.james.jellyfinlibrarymanager.helpers.VideoTranscodeJobBuilder_OLD;
import dev.partin.james.jellyfinlibrarymanager.repositories.JobDefinitionRepository;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.junit.After;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.partin.james.jellyfinlibrarymanager.helpers.MathHelpers.mode;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
class JellyfinLibraryManagerApplicationTests {

	@Autowired
	private JobDefinitionRepository jobDefinitionRepository;

	@Autowired
	private MockMvc mockMvc;

	private final File BigBuckBunny = new File("src/test/resources/big_buck_bunny.mp4");

	@Test
	void contextLoads() {
	}

	@Test
	void quickRegexTest() {
		int TFF = 0;
		int Progressive = 0;
		String testString = "[Parsed_idet_0 @ 0x600002684b00] Multi frame detection: TFF:     5 BFF:     0 Progressive:   376 Undetermined:   120";
		String regex = "TFF: (.*?) BFF";
		String regex2 = "Progressive: (.*?) Undetermined";
		Pattern pattern = Pattern.compile(regex);
		Pattern pattern2 = Pattern.compile(regex2);
		Matcher TFFmatcher = pattern.matcher(testString);
		Matcher ProgressiveMatcher = pattern2.matcher(testString);
		if (TFFmatcher.find()) {
			TFF = Integer.parseInt(TFFmatcher.group(1).trim());
		}
		if (ProgressiveMatcher.find()) {
			Progressive = Integer.parseInt(ProgressiveMatcher.group(1).trim());
		}
		assert TFF == 5;
		assert Progressive == 376;
	}

	@Test
	void stringParseTest() {
		String testString = "[Parsed_metadata_1 @ 0x600003b74210] lavfi.cropdetect.w=1712";
		int width = Integer.parseInt(testString.substring(testString.indexOf("=") + 1));
		assert width == 1712;
	}

	//TODO: Fix this test once batch is finished
	//@Test
	void testTranscode() throws IOException {
		//Add big buck bunny in the resources folder
		File testFile = new File("src/test/resources/big_buck_bunny.mp4");
		TranscodeConfiguration transcodeConfiguration = new TranscodeConfiguration();
		JobDefinition jobDefinition = new JobDefinition(testFile);
		jobDefinition.setTestMode(true);
		jobDefinitionRepository.save(jobDefinition);
		jobDefinition.setFilepath(testFile.getParent());
		jobDefinition.setFileName("/" + testFile.getName());
		transcodeConfiguration.setCodec("libx264");
		transcodeConfiguration.setCrf(30);
		transcodeConfiguration.setAudio_codec("aac");
		transcodeConfiguration.setAudio_bitrate(128);
		transcodeConfiguration.setAudio_channels(2);
		transcodeConfiguration.setAuto_crop(true);
		//1718x960
		//640x480
		transcodeConfiguration.setResolution(new int[]{640, 480});
		transcodeConfiguration.setAuto_deinterlace(true);
		FFmpegJob job = new VideoTranscodeJobBuilder_OLD(transcodeConfiguration, jobDefinition).build();
		job.run();
		jobDefinitionRepository.delete(jobDefinition);
	}

	@After
	void cleanup() {
		File testFile = new File("src/test/resources/big_buck_bunny.mp4");
		File testFile2 = new File("src/test/resources/big_buck_bunny[JobID=1].mp4");
		if (testFile2.exists()) {
			testFile2.delete();
		}
		var testJobs = jobDefinitionRepository.getTestJobs();
		jobDefinitionRepository.deleteAll(testJobs);
	}

	@Test
	void testMDAHashingSpeed() throws NoSuchAlgorithmException, IOException {
		float startTimeSeconds = System.nanoTime() / 1000000000f;
		boolean fileExists = BigBuckBunny.exists();
		long fileSize = BigBuckBunny.length();
		float l = (float) fileSize / 1024f / 1024f / 1024f;
		int repeat = (int) (100 / l);
		FileInputStream fileStream = new FileInputStream(BigBuckBunny);
		MessageDigest digest = MessageDigest.getInstance("MD5");
		long totalBytesRead = 0;
		while (repeat > 0) {
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = fileStream.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
				totalBytesRead += read;
			}
			byte[] md5sum = digest.digest();
			fileStream.reset();
			repeat--;
		}
		fileStream.close();
		float endTimeSeconds = System.nanoTime() / 1000000000f;
		assert (endTimeSeconds - startTimeSeconds) < 5;
	}

	@Test
	void testMode() {
		int[] testArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 9};
		assert mode(testArray) == 9;
	}
}
