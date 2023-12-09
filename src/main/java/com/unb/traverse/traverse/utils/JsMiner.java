package com.unb.traverse.traverse.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import lombok.val;

public class JsMiner {
	
    public static class FirstParentFilter extends RevFilter {
        private Set<RevCommit> ignoreCommits = new HashSet<>();

        @Override
        public boolean include(RevWalk revWalk, RevCommit commit) throws IOException {
            if (commit.getParentCount() > 1) {
                ignoreCommits.add(commit.getParent(1));
            }

            boolean include = true;
            if (ignoreCommits.contains(commit)) {
                include = false;
                ignoreCommits.remove(commit);
            }
            return include;
        }

        @Override
        public RevFilter clone() {
            return new FirstParentFilter();
        }
    }

	public static List<RevCommit> getCommits(Repository repository, Path repoPath, String startCommitId,
			String endCommitId) throws Exception {

		try (RevWalk revWalk = new RevWalk(repository)) {
			

			// Configura a ordem para cronológica reversa
			revWalk.sort(RevSort.TOPO);
			revWalk.sort(RevSort.REVERSE, true);
			
			// Obtém o commit mais recente (HEAD)
			// Marca os pontos de término (do mais antigo ao mais recente)
			RevCommit head = revWalk.parseCommit(repository.resolve("HEAD"));
			revWalk.markStart(head);
			
			revWalk.setRevFilter(AndRevFilter.create( new FirstParentFilter(), RevFilter.NO_MERGES ));

			List<RevCommit> revCommits = new ArrayList<>();

			// Itera sobre os commits em ordem cronológica
			for (RevCommit commit : revWalk) {
				if (commit.getParentCount() == 1) {
					revCommits.add(commit);
				}
			}

			return revCommits;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
