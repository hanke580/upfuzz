# TODOs

## Log the mutation if it fails
When doing a mutation, we randomly pick a parameter/command and mutate it. Since we apply many constraints to make the command valid, it's possible that the mutation could fail. Then we will redo the mutation a few times until we get a successful mutation. But we also need to pay attention to the failed mutations, since this could impact our correctness.
> E.g. The range for a parameter is {1, 2}, and we randomly pick a value from the set, like 1. However, the value is 1 already before the mutation. Thus the mutation does not have any effect. We consider it a useless mutation.
