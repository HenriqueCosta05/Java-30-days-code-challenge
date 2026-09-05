package adapters.cli;

import application.showuseractivity.ShowUserActivityResponse;
import java.time.Instant;
import java.util.List;

/**
 * Output boundary for the CLI: turns a use case response into the lines a
 * terminal reader sees.
 */
public interface ActivityPresenter {

    List<String> present(ShowUserActivityResponse response, Instant now);
}
