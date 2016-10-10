package nl.anhenet.syncres.view;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nl.anhenet.syncres.discover.ResultIndex;
import nl.anhenet.syncres.discover.ResultIndexPivot;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for viewing a ResultIndex as a tree.
 */
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.CLASS)
public class TreeBase {

  private List<TreeResultView> roots;

  public TreeBase(ResultIndex resultIndex) {
    init(resultIndex, new Interpreter());
  }

  public TreeBase(ResultIndex resultIndex, Interpreter interpreter) {
    init(resultIndex, interpreter);
  }

  private void init(ResultIndex resultIndex, Interpreter interpreter) {
    ResultIndexPivot pivot = new ResultIndexPivot(resultIndex);

    roots = pivot.listRsRootResultsByLevel(3).stream()
      .map(rsRootResult -> new TreeResultView(rsRootResult, interpreter))
      .collect(Collectors.toList());
  }

  public List<TreeResultView> getRoots() {
    return roots;
  }
}
