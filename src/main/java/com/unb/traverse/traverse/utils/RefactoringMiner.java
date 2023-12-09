package com.unb.traverse.traverse.utils;


import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.unb.traverse.traverse.utils.JsMiner.FirstParentFilter;

public class RefactoringMiner {

	public static List<RevCommit> getCommits(Repository repository, Path repoPath, String startCommitId, String endCommitId) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		ObjectId from = repository.resolve(startCommitId);
		ObjectId to = repository.resolve(endCommitId);
		try (Git git = new Git(repository)) {
			List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(from, to).setRevFilter(new FirstParentFilter()).call()
					.spliterator(), false)
//					.filter(r -> r.getParentCount() == 1)
			        .collect(Collectors.toList());
			Collections.reverse(revCommits);
			return revCommits;
		}
	}

}
