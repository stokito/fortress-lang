import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

class PrintIdeas extends Object {

    public static void main(String[] args) {

		FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
		FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
		FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);

		OutputStreamWriter outW = new OutputStreamWriter(fdOut, Charset
				.forName("UTF-8"));

		OutputStreamWriter errW = new OutputStreamWriter(fdErr, Charset
				.forName("UTF-8"));

		/* Begin write sequence */


		String s = "a very long string to print ∀ ∈ one piece";
		BufferedWriter bufferedOut1 = new BufferedWriter(outW,
				2 * s.length() + 3);
		String t = "another output string that should not be mixed";
		BufferedWriter bufferedOut2 = new BufferedWriter(outW,
				2 * t.length() + 3);
		
		try {
			bufferedOut1.write(s);
			bufferedOut2.write(s);
			bufferedOut1.write("; A");
			bufferedOut1.write(s, 1, s.length() - 1);
			bufferedOut2.write("; A");
			bufferedOut2.write(s, 1, s.length() - 1);
			bufferedOut2.write(".");
			bufferedOut1.write(".");
			bufferedOut1.newLine();
			bufferedOut2.newLine();
			bufferedOut1.flush();
			bufferedOut2.flush();
		}
		catch (IOException e) {System.err.println(e); System.exit(-1);}
		/* end write sequence */

		s = "an error message";
		BufferedWriter bufferedErr = new BufferedWriter(errW,
				2 * s.length() + 3);
		try {
			bufferedErr.write(s);
			bufferedErr.write("; A");
			bufferedErr.write(s, 1, s.length() - 1);
			bufferedErr.write(".");
			bufferedErr.newLine();
			bufferedErr.flush();
		}
		catch (IOException e) {System.err.println(e); System.exit(-1);}
		/* end write sequence */

	}
}
